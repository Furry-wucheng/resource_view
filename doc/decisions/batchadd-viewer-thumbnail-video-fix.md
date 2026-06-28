# batchadd-viewer-thumbnail-video-fix — 批量添加性能优化 + 查看器缩略图补全 + 视频支持 + 模式即时切换

> 时间: 2026-06-29 | Agent: opencode | 状态: ✅ 已完成 | 前置: home-grid-style-thumbnail-perf

## 设计决策

### D-001: BatchAddResourcesUseCase stat() 并行化
- **背景**: 串行 `for (path in paths)` 对 SMB 源每条 `stat()` 都是一次网络往返，添加 50 个资源需要 50 次串行网络调用
- **选择**: Phase 1 使用 `coroutineScope { paths.map { async { fileSource.stat(path) } }.awaitAll() }` 并行获取元数据；Phase 2 串行创建实体避免 SMB 共享连接并发问题
- **备选**: 整体并行（stat + createEntity）→ 放弃，`DetectOrganizationModeUseCase` 涉及 `listDirectory()` 在共享 SMB 连接上可能冲突
- **影响文件**: `domain/usecase/BatchAddResourcesUseCase.kt:60-75`

### D-002: 批量添加跳过组织模式深度检测
- **背景**: `DetectOrganizationModeUseCase` 对每个文件夹调用 `listDirectory()` 最深 3 层，在 SMB 上每条路径扩大为 2~N+1 次网络往返
- **选择**: 批量添加时（`organizationMode == null`），文件夹默认使用 `OrganizationMode.FLATGRID`，跳过深度检测。用户后续可在资源详情弹窗手动调整
- **备选**: 缓存检测结果 → 放弃，批量添加通常是一次性操作，缓存性价比低
- **影响文件**: `domain/usecase/BatchAddResourcesUseCase.kt:165-178`

### D-003: 缩略图异步生成不阻塞主流程
- **背景**: `generateThumbnails()` 使用 `coroutineScope { awaitAll() }` 阻塞等待所有缩略图完成才返回，用户等待数分钟看不到任何反馈
- **选择**: 新增 `thumbnailScope: CoroutineScope?` 参数。非 null 时用 `scope.launch(Dispatchers.IO)` fire-and-forget，资源立即显示；null 时保持同步行为向后兼容
- **备选**: 默认异步去掉同步路径 → 放弃，测试场景需要同步等待
- **影响文件**: `domain/usecase/BatchAddResourcesUseCase.kt:82-109`, `ui/screens/sources/FileBrowserViewModel.kt:205-233`

### D-004: HomeViewModel 缺失缩略图后台补全
- **背景**: 用户可能在不同时机添加资源（通过文件浏览器批量添加、或缩略图生成失败），回到主页时部分资源无缩略图显示 fallback 图标
- **选择**: `HomeViewModel.init` 中非阻塞调用 `generateMissingThumbnails()`。收集 `thumbnailPath == null` 的资源，通过 `ThumbnailRepository` 和 `ThumbnailTaskPool` 并发生成，完成后 Room Flow 自动刷新 UI。依赖通过 Koin 可选注入（null 时跳过）
- **备选**: 在 `HomeScreen` 的 `LaunchedEffect` 中触发 → 放弃，ViewModel init 更早触发且不会因 recomposition 重复执行
- **影响文件**: `ui/screens/home/HomeViewModel.kt:186-265`, `di/ViewModelModule.kt:26`

### D-005: 查看器缩略图改用 FileEntryThumbnailLoader
- **背景**: `ChapterListScreen` 用 Coil `AsyncImage` + 裸相对路径加载章节封面，`ContentGridScreen` 的 `GridEntryCard` 只显示图标。两者都不走 `FileEntryThumbnailLoader`，导致缩略图不显示
- **选择**: ViewModel 注入 `FileBrowserThumbnailDiskCache`，添加 `loadChapterCover()`/`loadEntryThumbnail()` 方法。Screen 端用 `produceState<Bitmap>` 加载，fallback 显示类型颜色+图标。与文件浏览器 `FileEntryGridItem` 缩略图逻辑一致
- **影响文件**: `ChapterListViewModel.kt`, `ContentGridViewModel.kt`, `ChapterListScreen.kt`, `ContentGridScreen.kt`

### D-006: OrgModeSwitcher 接入导航实现即时切换
- **背景**: `OrgModeSwitcher` 只调 `changeOrganizationMode()` 写 DB 不触发导航。用户点击平铺后仍在章节列表
- **选择**: 新增 `onNavigateToMode: (OrganizationMode) -> Unit` 回调穿透至 `AppNavGraph`。切换时 `navController.navigate(route) { popUpTo(Home) }` 替换回退栈
- **影响文件**: `ChapterListScreen.kt`, `ContentGridScreen.kt`, `AppNavGraph.kt`

### D-007: OrganizationStrategy 纳入视频文件
- **背景**: 4 个策略全部只用 `MediaFormats.imageExtensions` 过滤，视频文件在任何组织模式下不可见
- **选择**: `FlatGridStrategy`/`GalleryStrategy` 的 `getContents()` 加入 `videoExtensions`；`ChapterStrategy`/`ChapterGalleryStrategy` 的 `fileCount` 计入视频、`coverPath` 图片优先视频回落
- **影响文件**: `FlatGridStrategy.kt`, `GalleryStrategy.kt`, `ChapterStrategy.kt`, `ChapterGalleryStrategy.kt`

### D-008: 视频条目导航到 FileViewer
- **背景**: 视频在 ContentGrid/ChapterList 可见后，点击走 `SequenceViewer` → `ImageFolderProvider`（只看图），无法播放
- **选择**: 新增 `onOpenVideo: (sourceId, filePath) -> Unit` 回调。`MediaFormats.isVideo()` 检测，点击导航到 `Screen.FileViewer` → `loadFromSource()` → `MixedFolderProvider`
- **影响文件**: `ContentGridScreen.kt`, `ChapterListScreen.kt`, `AppNavGraph.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | stat() 并行化、跳过深度组织检测、thumbnailScope 异步缩略图 |
| `ui/screens/home/HomeViewModel.kt` | ✏️ 修改 | 新增 generateMissingThumbnails() 后台补全缩略图 |
| `di/ViewModelModule.kt` | ✏️ 修改 | HomeViewModel/ChapterListVM/ContentGridVM 注入缩略图依赖 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | confirmBatchAdd 传入 viewModelScope 启用异步缩略图 |
| `.../FileBrowserViewModelTest.kt` | ✏️ 修改 | mock 适配新增 thumbnailScope 参数 |
| `ui/screens/viewer/ChapterListViewModel.kt` | ✏️ 修改 | 注入 diskCache + loadChapterCover() + sourceId |
| `ui/screens/viewer/ContentGridViewModel.kt` | ✏️ 修改 | 注入 diskCache + loadEntryThumbnail() + listFlat 纳视频 |
| `ui/screens/viewer/ChapterListScreen.kt` | ✏️ 修改 | AsyncImage→produceState + onNavigateToMode + onOpenVideo |
| `ui/screens/viewer/ContentGridScreen.kt` | ✏️ 修改 | GridEntryCard→缩略图卡片 + onNavigateToMode + onOpenVideo |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 接线 onNavigateToMode + onOpenVideo |
| `shared/organization/FlatGridStrategy.kt` | ✏️ 修改 | getContents() 纳入 videoExtensions |
| `shared/organization/GalleryStrategy.kt` | ✏️ 修改 | collectMedia() 纳入 videoExtensions |
| `shared/organization/ChapterStrategy.kt` | ✏️ 修改 | fileCount 计入视频，coverPath 视频回落 |
| `shared/organization/ChapterGalleryStrategy.kt` | ✏️ 修改 | 同上 |
| `.../FlatGridStrategyTest.kt` | ✏️ 更新 | 验证视频纳入 + 更新断言 |
| `.../GalleryStrategyTest.kt` | ✏️ 更新 | 验证视频纳入 + 更新断言 |
