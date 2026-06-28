# 2026-06-29 — 视频播放 + 查看器无缝切换 + 缩略图缓存统一修复

> 时间: 2026-06-29 | Agent: opencode | 状态: ✅ 已完成 | 前置: batchadd-viewer-thumbnail-video-fix, refactor-unified-thumbnail

## 设计决策

### D-001: ExoPlayer 生命周期管理 — 复用实例 + Pager 非当前页不创建

- **背景**: 上次修复（92e799b）将 ExoPlayer 从 ViewModel 迁移到 Composable 生命周期，解决了"从不释放"问题。但 `remember(videoItem.videoSource)` 导致切换视频时创建新 ExoPlayer，旧 player 的 `release()` 异步未完成时新 player 已 `prepare()` → 两个硬解码器争抢 → NO_MEMORY。同时 HorizontalPager 滑动动画期间新旧两页 VideoPageContent 共存。
- **选择**: ① `remember { }` 无 key，复用同一 ExoPlayer；② `loadMedia()` 开头调 `stop()` + `clearMediaItems()` 同步释放旧解码器后加载新源；③ Pager 传 `isPageSelected=false` 时渲染黑色占位 Box，不创建 ExoPlayer
- **影响文件**: `VideoPlayerController.kt:52-67`, `ViewerScreen.kt:363-382`, `ViewerScreen.kt:312-325`
- **被依赖**: 所有视频播放场景

### D-002: 查看器无缝切换 — MixedFolderProvider 全覆盖

- **背景**: `loadVideoResource()` 只创建单视频 item（`totalPages=1`），无法翻页；`loadContentProviderResource()` 只用 `ImageFolderProvider`，排除视频；`MixedFolderProvider` 不支持 GALLERY 递归扫描
- **选择**: ① `loadVideoResource()` 改用 MixedFolderProvider 加载父目录，通过 `findIndex()` 定位到当前视频，支持翻页到同目录图片/视频；② `loadContentProviderResource()` 非 PDF 资源改用 MixedFolderProvider，通过 `buildViewerItems()` 构建混合列表；③ MixedFolderProvider 加 `recursive` 参数，GALLERY 模式下递归扫描子目录
- **影响文件**: `ViewerViewModel.kt:357-470`, `MixedFolderProvider.kt:19-72`
- **被依赖**: 资源库查看器所有入口

### D-003: 缩略图异常日志可见性

- **背景**: 缩略图生成整条链路（Loader → Generator → Repository → UseCase → ViewModel）有 6 层异常 catch 块完全无日志，Bitmap recycle 后写入 disk cache 导致 `IllegalStateException` 也静默吞没，开发者无法诊断任何失败原因
- **选择**: ① 所有 catch 加 `Log.e(TAG, msg, e)`；② `BatchAddResourcesUseCase`/`HomeViewModel` 的 `Result.Ok(null)` 分支加 `Log.w`；③ `ThumbnailRepository` 无生成器命中加 `Log.w`；④ `ImageThumbnailGenerator`/`PdfThumbnailGenerator` 的 `diskCache.put()` 移到 `saveBitmap()` 之前，避免 bitmap 被 recycle 后再写入
- **影响文件**: `FileEntryThumbnailLoader.kt:34`, `ImageThumbnailGenerator.kt:82-84`, `PdfThumbnailGenerator.kt:88-95,99`, `VideoThumbnailGenerator.kt:100`, `BatchAddResourcesUseCase.kt:163-166`, `HomeViewModel.kt:329-337`, `ThumbnailRepository.kt:39`
- **被依赖**: 所有缩略图生成场景的 Debug 能力

### D-004: 缩略图缓存统一 — ThumbnailLoadManager 单例化

- **背景**: 三个 ViewModel（FileBrowser / ContentGrid / ChapterList）各自创建独立的 `ThumbnailLoadManager` 实例，各自有独立的内存 LRU（32），磁盘缓存的 key 参数也不一致：FileBrowser 用 Source UUID，ContentGrid 用 Resource UUID，导致同一张磁盘缓存永远无法命中
- **选择**: ① `ThumbnailLoadManager` 注册为 Koin `single` 单例，三个 ViewModel 共享同一内存 LRU 和磁盘缓存；② `sourceId` 从构造参数移除，改为 `load(sourceId, entry, policy)` 方法参数，ContentGrid/ChapterList 传 `resource.sourceId`；③ 内存 LRU key 改为 `"$sourceId:${entry.relativePath}"`，避免不同源同名文件串扰；④ setFileSource() 方法注入 FileSource
- **影响文件**: `ThumbnailLoadManager.kt`, `CoilModule.kt`, `ViewModelModule.kt`, `ContentGridViewModel.kt`, `ChapterListViewModel.kt`, `FileBrowserViewModel.kt`
- **被依赖**: 所有浏览模式的缩略图加载

### D-005: 章廊 vs 章节缩略图查找策略区分

- **背景**: 章廊（CHAPTER_GALLERY）跨目录收集文件，应该用 `RESOURCE_COVER`（递归查找预览图），与封面生成逻辑一致；章节（CHAPTER）只看子文件夹的第一层文件，应该用 `DIRECT_CHILD`
- **选择**: `ChapterListViewModel.loadChapterCover()` 根据 `organizationMode` 选择 policy：`CHAPTER_GALLERY` → `RESOURCE_COVER`，`CHAPTER` → `DIRECT_CHILD`。同时用 `fileSource.stat(path)` 获取真实 FileEntry（含实际文件大小），避免 size=0 导致磁盘缓存 key 与其他模式不匹配
- **影响文件**: `ChapterListViewModel.kt:151-167`
- **被依赖**: ChapterList 缩略图显示

### D-006: CancellationException 不透传 misses 集合

- **背景**: `ThumbnailLoadManager.load()` 的 catch 块吞所有异常并加入 `misses` 集合。用户切页面 → ViewModel 协程取消 → `CancellationException` 被吞 → 缩略图被标记为"永久缺失" → 回来不再重试
- **选择**: catch 块中检测 `e is CancellationException` → 直接 `throw e`，不加入 misses。真实失败（网络超时、解码失败）仍正常加入 misses 避免无限重试
- **影响文件**: `ThumbnailLoadManager.kt:70-73`
- **被依赖**: 所有浏览模式缩略图加载的容错能力

### D-007: FileBrowserThumbnailDiskCache key 精简

- **背景**: 磁盘缓存 key 指纹包含 `entry.modifiedAt`，导致 `ImageThumbnailGenerator` 生成封面时写入的缓存（使用 `resource.updatedAt`）和 ContentGrid 浏览时查盘（使用 `listDirectory` 返回的 `entry.modifiedAt`）产生不同 key，永远无法命中
- **选择**: 从 key 指纹中移除 `modifiedAt`，仅保留 `CACHE_VERSION | sourceId | relativePath | size | policy`。文件大小变化已足以触发缓存失效
- **影响文件**: `FileBrowserThumbnailDiskCache.kt:92-103`
- **被依赖**: 封面缓存与浏览缓存的互命中

### D-008: 清缓存后自动重生成封面

- **背景**: `HomeViewModel.generateMissingThumbnails()` 只检查 DB 字段 `thumbnailPath.isNullOrBlank()`，不检查文件实际是否存在。用户清除缓存后 DB 字段仍指向已删除文件，永远不会触发生成
- **选择**: filter 条件增加 `|| !java.io.File(path).exists()`，缓存文件删除后自动触发重新生成
- **影响文件**: `HomeViewModel.kt:308-311`
- **被依赖**: 首页封面显示正确性

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/viewer/VideoPlayerController.kt` | ✏️ 修改 | +stop(), loadMedia() 开头调 stop() |
| `ui/screens/viewer/ViewerScreen.kt` | ✏️ 修改 | ExoPlayer 复用 + isPageSelected |
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ 修改 | MixedFolderProvider 全覆盖 + 无缝切换 |
| `shared/content/MixedFolderProvider.kt` | ✏️ 修改 | +recursive 参数 |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | ✏️ 修改 | +Log.e, +TAG |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ 修改 | +Log.e, saveBitmap 调序, BFS 复用, +TAG |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | ✏️ 修改 | +Log.e, saveBitmap 调序, +TAG |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ 修改 | e.printStackTrace→Log.e, +TAG |
| `shared/thumbnail/FileBrowserThumbnailDiskCache.kt` | ✏️ 修改 | key 移除 modifiedAt |
| `shared/thumbnail/ThumbnailLoadManager.kt` | ✏️ 修改 | Koin single, load(sourceId,entry,policy), CancellationException |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | null 分支 +Log.w |
| `data/repository/ThumbnailRepository.kt` | ✏️ 修改 | 无生成器 +Log.w, +TAG |
| `ui/screens/home/HomeViewModel.kt` | ✏️ 修改 | filter 加文件存在检查, null 分支 +Log.w |
| `di/CoilModule.kt` | ✏️ 修改 | +ThumbnailLoadManager 单例 |
| `di/ViewModelModule.kt` | ✏️ 修改 | 三个 VM 注入共享实例 |
| `ui/screens/viewer/ContentGridViewModel.kt` | ✏️ 修改 | 共享 LoadManager, resource.sourceId |
| `ui/screens/viewer/ChapterListViewModel.kt` | ✏️ 修改 | 共享 LoadManager, policy 区分, 真 FileEntry |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 共享 LoadManager |
| `ui/screens/viewer/VideoPlayerControllerTest.kt` | ✏️ 修改 | +stop/切换测试 |
| `ui/screens/viewer/ViewerViewModelTest.kt` | ✏️ 修改 | 适配新构造 |
| `ui/screens/sources/FileBrowserViewModelTest.kt` | ✏️ 修改 | +mock ThumbnailLoadManager |

## 已知问题 / TODO

- [ ] Archive 资源类型尚无 ThumbnailGenerator，添加 Archive 资源时封面静默跳过
- [ ] PdfThumbnailGenerator 全量下载 PDF，大文件 SMB 超时无进度反馈
