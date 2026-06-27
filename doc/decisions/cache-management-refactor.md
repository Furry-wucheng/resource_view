# 缓存管理重构 — 独立容量控制 + 预加载优化 + 缩略图复用

> 时间: 2026-06-27 | Agent: opencode | 状态: ✅ 已完成

## Issue

> 日期: 2026-06-27 | 类型: feature | 状态: ✅ 已完成

### 描述

对查看器预加载策略、缩略图缓存系统和设置页面缓存管理进行全面重构：

1. **预加载策略优化**：将查看器预加载范围从 ±2 页改为 +3 -1 页（后 3 页 + 前 1 页），提升正向阅读流畅度
2. **缩略图尺寸统一**：封面生成器尺寸从 300px 统一为 320px
3. **缩略图缓存复用**：Generator 生成前先检查 `FileBrowserThumbnailDiskCache`，命中则直接复用，避免重复 I/O
4. **封面并发生成**：`BatchAddResourcesUseCase` 使用 `ThumbnailTaskPool` + `coroutineScope` 并发生成，遵循设置中的 `thumbnailConcurrency` 配置
5. **缓存管理重构**：三个独立缓存（封面/页面/缩略图）分别管理容量，设置页面显示进度条 + 容量选择器

### 验收标准

- [x] 查看器预加载范围为 +3 -1（后 3 页 + 前 1 页）
- [x] 所有缩略图生成器尺寸统一为 320px
- [x] Generator 生成前检查 `FileBrowserThumbnailDiskCache`，命中则复用
- [x] 批量添加资源时并发生成缩略图，遵循 `thumbnailConcurrency` 配置
- [x] 封面缓存目录移出 `image_cache/` 到 `thumbnails/resources/`
- [x] 设置页面显示三个独立缓存条目，各有进度条和容量选择器
- [x] 封面缓存默认无限制（永久），可设置容量
- [x] 页面缓存和缩略图缓存独立设置容量
- [x] 自定义容量内嵌输入框，非弹窗
- [x] build/test passed

---

## 设计决策

### D-001: 预加载策略从 ±2 改为 +3 -1

- **背景**: PRD 要求"当前页 + 前 1 页 + 后 2 页"，但实际实现为 ±2（前后各 2 页），比 PRD 多预取了 `currentPage - 2`
- **选择**: 改为 +3 -1（后 3 页 + 前 1 页），更激进的正向预取，符合漫画/图片阅读习惯
- **备选**: 保持 ±2 或改为 PRD 的 +2 -1
- **影响文件**: `ui/screens/viewer/ViewerViewModel.kt:591-596`

```kotlin
// 改前
val pageOrder = listOf(current.pageIndex + 1, current.pageIndex - 1, 
                       current.pageIndex + 2, current.pageIndex - 2)
// 改后
val pageOrder = listOf(current.pageIndex + 1, current.pageIndex - 1, 
                       current.pageIndex + 2, current.pageIndex + 3)
```

### D-002: 缩略图尺寸统一为 320px

- **背景**: `ImageThumbnailGenerator` 和 `PdfThumbnailGenerator` 使用 300px，`VideoThumbnailGenerator` 和 `FileBrowserThumbnailDiskCache` 使用 320px
- **选择**: 统一为 320px，减少同一文件因尺寸不同导致的缓存失效
- **备选**: 统一为 300px
- **影响文件**: `shared/thumbnail/ImageThumbnailGenerator.kt:23`, `shared/thumbnail/PdfThumbnailGenerator.kt:22`

### D-003: Generator 注入 FileBrowserThumbnailDiskCache 实现复用

- **背景**: 文件浏览器和封面生成可能对同一文件生成缩略图，造成重复 I/O
- **选择**: Generator 构造函数注入 `FileBrowserThumbnailDiskCache?`，生成前先检查缓存，命中则直接复用
- **备选**: 在 ThumbnailRepository 层面处理复用（需要修改接口签名）
- **影响文件**: `shared/thumbnail/ImageThumbnailGenerator.kt`, `shared/thumbnail/PdfThumbnailGenerator.kt`, `shared/thumbnail/VideoThumbnailGenerator.kt`, `di/RepositoryModule.kt`

```kotlin
class ImageThumbnailGenerator(
    private val context: Context,
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
) : ThumbnailGenerator {
    override suspend fun generate(...): File? {
        // 1. 检查封面缓存是否已存在
        if (outputFile.exists()) return outputFile
        // 2. 检查 FileBrowserThumbnailDiskCache
        val cached = thumbnailDiskCache?.get(sourceId, entry, RESOURCE_COVER)
        if (cached.isCached && cached.bitmap != null) { ... }
        // 3. 生成新的缩略图
        ...
    }
}
```

### D-004: 封面缓存目录移出 image_cache/

- **背景**: `image_cache/` 目录被"清理全部缓存"按钮删除，封面缓存也会被误删
- **选择**: 封面缓存目录改为 `thumbnails/resources/`，独立于 `image_cache/`
- **备选**: 保持在 `image_cache/resources/`，但修改清理逻辑排除封面
- **影响文件**: `domain/usecase/BatchAddResourcesUseCase.kt:105`

### D-005: 批量添加并发生成缩略图

- **背景**: `BatchAddResourcesUseCase.generateThumbnails()` 是串行 for 循环，效率低
- **选择**: 使用 `ThumbnailTaskPool` + `coroutineScope` + `async` 并发生成，遵循设置中的 `thumbnailConcurrency` 配置
- **备选**: 固定并发数（不读取配置）
- **影响文件**: `domain/usecase/BatchAddResourcesUseCase.kt:101-121`

```kotlin
private suspend fun generateThumbnails(entities, fileSource) {
    val config = database.appConfigDao().getConfig().first()
    val concurrency = config?.thumbnailConcurrency ?: 4
    val pool = ThumbnailTaskPool(concurrency)
    coroutineScope {
        entities.map { entity ->
            async { pool.run { ... } }
        }.awaitAll()
    }
}
```

### D-006: 三个独立缓存容量设置

- **背景**: 原来只有一个 `cacheLimitMB` 控制所有缓存，无法精细管理
- **选择**: 数据库新增 `coverCacheLimitMB`（默认 0=无限制）、`pageCacheLimitMB`（默认 500）、`thumbnailCacheLimitMB`（默认 500）三个字段，设置页面三个独立条目
- **备选**: 保持单一 `cacheLimitMB`
- **影响文件**: `data/local/entity/AppConfigEntity.kt`, `domain/model/AppConfig.kt`, `data/local/AppDatabase.kt`, `ui/screens/settings/SettingsViewModel.kt`, `ui/screens/settings/SettingsScreen.kt`

### D-007: 自定义容量内嵌输入框

- **背景**: 原来的自定义容量使用弹窗对话框，交互不够直观
- **选择**: 自定义做成 FilterChip 样式，点击展开内嵌输入框
- **备选**: 保持弹窗对话框
- **影响文件**: `ui/screens/settings/SettingsScreen.kt`

---

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ 修改 | 预加载 +3 -1，使用 pageCacheLimitMB |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ 修改 | 尺寸 320 + 复用检测 + 注入 diskCache |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | ✏️ 修改 | 尺寸 320 + 复用检测 + 注入 diskCache |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ 修改 | 复用检测 + 注入 diskCache |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | 并发生成 + ThumbnailTaskPool |
| `data/cache/CacheManager.kt` | 🆕 新增 | 统一缓存管理 |
| `di/CacheModule.kt` | 🆕 新增 | 注册 CacheManager |
| `data/local/entity/AppConfigEntity.kt` | ✏️ 修改 | 新增三个缓存容量字段 |
| `domain/model/AppConfig.kt` | ✏️ 修改 | 同步更新 |
| `data/local/AppDatabase.kt` | ✏️ 修改 | v3→v4 迁移 |
| `data/local/migration/DatabaseMigrator.kt` | ✏️ 修改 | 注册迁移 |
| `ui/screens/settings/SettingsViewModel.kt` | ✏️ 修改 | 三个独立缓存容量管理 |
| `ui/screens/settings/SettingsScreen.kt` | ✏️ 修改 | 三个独立缓存设置条目 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 使用 thumbnailCacheLimitMB |
| `di/CoilModule.kt` | ✏️ 修改 | 使用 thumbnailCacheLimitMB |
| `di/RepositoryModule.kt` | ✏️ 修改 | 注入 diskCache + database |
| `di/ViewModelModule.kt` | ✏️ 修改 | 注入 CacheManager |
| `ResourceViewerApp.kt` | ✏️ 修改 | 注册 cacheModule |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 缓存目录结构

```
thumbnails/resources/     - 封面缓存（默认永久，可设置容量）
image_cache/pages/        - SMB 页面缓存
image_cache/file_browser/ - 缩略图缓存
image_cache/coil/         - Coil 缓存（跟随缩略图容量设置）
```

## 已知问题 / TODO

- [ ] Generator 复用检测目前只检查 RESOURCE_COVER 策略的缓存，DIRECT_CHILD 策略的缓存无法复用（key 包含 policy）
- [ ] 页面缓存容量设置目前只在 ViewerViewModel 创建 Provider 时读取一次，运行中修改不会实时生效
