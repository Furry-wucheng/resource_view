# refactor-unified-thumbnail — 统一缩略图加载管理器和卡片组件

> 时间: 2026-06-29 | Agent: opencode | 状态: ✅ 已完成 | 前置: batchadd-viewer-thumbnail-video-fix

## 设计决策

### D-001: ThumbnailLoadManager 统一三处 ViewModel 缓存逻辑
- **背景**: `FileBrowserViewModel`、`ContentGridViewModel`、`ChapterListViewModel` 各自维护一份完全相同的缩略图加载代码（LinkedHashMap LRU → misses Set → diskCache → FileEntryThumbnailLoader → 回写），仅缓存大小和并发数两个参数不同。总计 ~240 行 copy-paste
- **选择**: 提取 `ThumbnailLoadManager(sourceId, loader, diskCache, maxConcurrency, maxCacheSize)`。三个 ViewModel 各自创建实例，`loadThumbnail()` 方法简化为一行委托。FileBrowser 保留 `inFlightThumbnails` dedup 包装层
- **备选**: 不做提取，保持各自维护 → 放弃，已有统计 bug（ContentGrid/ChapterList 并发数硬编码 4 不读配置）需要改三份代码
- **影响文件**: `shared/thumbnail/ThumbnailLoadManager.kt`, 三个 ViewModel

### D-002: FileThumbnailCard 统一两处网格卡片 UI
- **背景**: `FileBrowserScreen` 的 `FileEntryGridItem` 和 `ContentGridScreen` 的 `GridEntryCard` 实现完全一致（3:4 Card + 渐变遮罩 + 类型颜色 fallback + white 标题），但是两个独立的 private 函数，无法共享
- **选择**: 提取 `FileThumbnailCard` 到 `ui/components/`。通过 callback `loadThumbnail: suspend (FileEntry) -> Bitmap?` 注入缩略图加载逻辑。提供三个 slot（leadingIcon / trailingIcon / bottomEndBadge）供调用方注入多选标记、收藏星标、文件夹角标
- **备选**: 继续各自维护 → 放弃，样式 bug 需修两处
- **影响文件**: `ui/components/FileThumbnailCard.kt`, `FileBrowserScreen.kt`, `ContentGridScreen.kt`

### D-003: fileTypeColor / fileTypeIcon 提取为公共工具函数
- **背景**: FileBrowserScreen 的 `fileTypeColor(FileEntry)` 和 `fileIcon(FileEntry)` 是 private 函数，但 ContentGridScreen 和 ChapterListScreen 也实现了同样的逻辑
- **选择**: 在 `FileThumbnailCard.kt` 中定义为 top-level public 函数。FileBrowserScreen 删除自己的 private 版本，改为 import。ContentGridScreen 直接使用 `FileThumbnailCard` 内置的 fallback 逻辑无需单独引用
- **影响文件**: `ui/components/FileThumbnailCard.kt`, `FileBrowserScreen.kt`

### D-004: ContentGrid/ChapterList 并发数读取配置
- **背景**: 两个 ViewModel 的 `ThumbnailTaskPool(4)` 硬编码并发数为 4，不读取用户配置的 `thumbnailConcurrency`。FileBrowser 正确读取了配置并做了 SMB 减半，但另外两处没有
- **选择**: `ThumbnailLoadManager` 接受 `maxConcurrency` 构造参数。ViewModel 在 `load()` 中从 `AppConfigDao.getConfig().first()` 读取配置后传入。`ViewModelModule` 注入 `AppConfigDao`
- **影响文件**: `ContentGridViewModel.kt`, `ChapterListViewModel.kt`, `di/ViewModelModule.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/thumbnail/ThumbnailLoadManager.kt` | 🆕 新增 | 统一内存 LRU + misses 记录 + 磁盘缓存 + 并发控制 |
| `ui/components/FileThumbnailCard.kt` | 🆕 新增 | 统一 3:4 Card + 渐变遮罩 + slot 位 + fileTypeColor/Icon |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 重构 | 替换缓存层为 ThumbnailLoadManager，保留 inFlight dedup |
| `ui/screens/viewer/ContentGridViewModel.kt` | ✏️ 重构 | 替换缓存层为 ThumbnailLoadManager，注入 AppConfigDao |
| `ui/screens/viewer/ChapterListViewModel.kt` | ✏️ 重构 | 替换缓存层为 ThumbnailLoadManager，注入 AppConfigDao |
| `ui/screens/sources/FileBrowserScreen.kt` | ✏️ 重构 | FileEntryGridItem → FileThumbnailCard |
| `ui/screens/viewer/ContentGridScreen.kt` | ✏️ 重构 | GridEntryCard → FileThumbnailCard |
| `di/ViewModelModule.kt` | ✏️ 修改 | ChapterListVM/ContentGridVM 注入 AppConfigDao |
