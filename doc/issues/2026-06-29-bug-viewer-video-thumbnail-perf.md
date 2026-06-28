# bug+feature: 查看器视频支持 + 缩略图补全 + 组织模式即时切换 + 批量添加性能优化

> 日期: 2026-06-29 | 类型: bug+feature+perf | 状态: ✅ 已完成

## 现象

1. **FlatGrid/Gallery/ChapterList 中视频文件完全不可见**：4 个 OrganizationStrategy 全部只过滤 `imageExtensions`，视频被丢弃。
2. **查看器缩略图不显示**：ChapterListScreen 和 ContentGridScreen 分别用 Coil `AsyncImage` + 裸路径 / 纯图标，不走 `FileEntryThumbnailLoader`。
3. **组织模式切换不能即时生效**：`OrgModeSwitcher` 只写 DB 不触发导航，切换后画面不动。
4. **批量添加资源慢**：`stat()` 串行 + 组织模式深度检测 + 缩略图阻塞 `awaitAll()`。
5. **首页缩略图不显示**：Coil 3 不支持裸 String 路径；无缩略图时无回落展示。
6. **视频缩略图 OOM**：`VideoThumbnailGenerator` 全量 `readFile()` 大视频文件。

## 复现步骤

1. 在文件浏览器中导入包含视频文件的文件夹
2. 回到首页观察缩略图
3. 点击资源进入平铺/画廊/章节模式 → 视频不出现
4. 切换组织模式 → 画面不切换

## 期望效果

- 视频文件在所有组织模式下可见，点击能播放
- 章节/内容网格缩略图正常显示（通过 FileEntryThumbnailLoader）
- 组织模式切换立即导航到对应视图
- 批量添加资源快速返回，缩略图后台异步生成
- 首页缩略图正常显示（Coil `File` 对象），无图时显示类型图标回落

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 A | `FlatGridStrategy/GalleryStrategy/ChapterStrategy/ChapterGalleryStrategy` 只用 `imageExtensions` 过滤 |
| 根因 B | `ChapterListScreen/ContentGridScreen` 缩略图不走 `FileEntryThumbnailLoader`（Coil 裸字符串 / 纯图标） |
| 根因 C | `OrgModeSwitcher` 只调 `changeOrganizationMode` 不触发导航 |
| 根因 D | `BatchAddResourcesUseCase` 串行 stat() + 缩略图阻塞 awaitAll() |
| 根因 E | Coil 3 `data(String)` 不认裸文件路径 |
| 根因 F | `VideoThumbnailGenerator` 全量 readFile |
| 修改文件 | 见产出清单 |
| 影响 stage | M20(策略), M21(章节), M23(首页), M27(批量添加), fix(缩略图) |

## 是否需合并回原文档

[x] 否 → 独立提案，不回溯

## 执行计划

### 第一轮：首页样式 + 缩略图修复 + 视频缩略图性能（opencode/clever-planet 完成）
1. 重构 `ResourceGridItem` 为 Card 卡片 + 类型回落 + Coil `File` 修复
2. 改写 `VideoThumbnailGenerator` 使用 `FileSourceMediaDataSource` 分块读取
3. 清理 `ImageThumbnailGenerator` 死代码
4. 提升 `FileSourceMediaDataSource` 可见性为 `internal`

### 第二轮：批量添加性能优化
5. `stat()` 并行化：Phase 1 `coroutineScope { async { } }` 并行取元数据
6. 批量添加跳过 `DetectOrganizationModeUseCase` 深度检测，默认 `FLATGRID`
7. 缩略图异步化：`thumbnailScope` 参数 → fire-and-forget 后台生成
8. `HomeViewModel` 新增 `generateMissingThumbnails()` 后台补全

### 第三轮：查看器缩略图 + 组织模式切换
9. `ChapterListViewModel`/`ContentGridViewModel` 注入 `FileBrowserThumbnailDiskCache`
10. `ChapterListScreen`/`ContentGridScreen` 缩略图改为 `produceState` + Bitmap 加载
11. `OrgModeSwitcher` 回调接入导航（`onNavigateToMode`）

### 第四轮：查看器视频支持
12. 4 个 OrganizationStrategy 的过滤逻辑纳入 `videoExtensions`
13. `ContentGridScreen`/`ChapterListScreen` 视频条目点击走 `FileViewer`

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/ResourceGridItem.kt` | ✏️ 重写 | Card 卡片、3:4、类型回落、Coil File 修复 |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ 重写 | FileSourceMediaDataSource 分块读取 |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ 清理 | 移除 decodeAndScale 死代码 |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | ✏️ 修改 | FileSourceMediaDataSource private→internal |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | stat()并行化 + 跳过深度检测 + thumbnailScope 异步 |
| `ui/screens/home/HomeViewModel.kt` | ✏️ 修改 | generateMissingThumbnails() 后台补全 |
| `di/ViewModelModule.kt` | ✏️ 修改 | HomeViewModel/ChapterListVM/ContentGridVM 注入新依赖 |
| `ui/screens/viewer/ChapterListViewModel.kt` | ✏️ 修改 | 注入 diskCache + loadChapterCover() + sourceId |
| `ui/screens/viewer/ContentGridViewModel.kt` | ✏️ 修改 | 注入 diskCache + loadEntryThumbnail() + sourceId + listFlat 纳视频 |
| `ui/screens/viewer/ChapterListScreen.kt` | ✏️ 修改 | AsyncImage→produceState + onNavigateToMode + onOpenVideo |
| `ui/screens/viewer/ContentGridScreen.kt` | ✏️ 修改 | GridEntryCard→缩略图卡片 + onNavigateToMode + onOpenVideo |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | confirmBatchAdd 传入 viewModelScope |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 接线 onNavigateToMode + onOpenVideo |
| `shared/organization/FlatGridStrategy.kt` | ✏️ 修改 | getContents() 纳入 videoExtensions |
| `shared/organization/GalleryStrategy.kt` | ✏️ 修改 | collectMedia() 纳入 videoExtensions |
| `shared/organization/ChapterStrategy.kt` | ✏️ 修改 | fileCount 计入视频，coverPath 视频回落 |
| `shared/organization/ChapterGalleryStrategy.kt` | ✏️ 修改 | 同上 |
| `.../FlatGridStrategyTest.kt` | ✏️ 更新 | 验证视频纳入 |
| `.../GalleryStrategyTest.kt` | ✏️ 更新 | 验证视频纳入 |
| `.../VideoThumbnailGeneratorTest.kt` | ✏️ 更新 | 验证不用 readFile |
| `.../FileBrowserViewModelTest.kt` | ✏️ 修改 | mock 适配 thumbnailScope |
