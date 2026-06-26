# M19 — ExoPlayer 视频播放器组件

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成

## 设计决策

### D-001: VideoPlayerViewModel 架构
- **背景**: 需要管理 ExoPlayer 实例并提供播放控制
- **选择**: VideoPlayerViewModel 继承 ViewModel，接收 ExoPlayer 实例（Koin 注入）
- **备选**: 在 ViewModel 内部创建 ExoPlayer（不易测试）
- **影响文件**: `ui/screens/viewer/VideoPlayerViewModel.kt`

### D-002: VideoMediaSource.SmbFile 携带 DataSource.Factory
- **背景**: SMB 视频需要自定义 DataSource，但 domain model 不应依赖 Source + password
- **选择**: SmbFile 携带调用方创建的 DataSource.Factory，而非 FileSource
- **备选**: 在 VideoPlayerViewModel 中创建 SmbDataSourceFactory（需要额外查询 Source + password）
- **影响文件**: `domain/model/ViewerItem.kt`, `ui/screens/viewer/VideoPlayerViewModel.kt`

### D-003: 视频资源检测策略
- **背景**: ViewerViewModel 需要区分视频和图片资源
- **选择**: 在 loadResource() 中检查 resource.type == VIDEO，分支处理
- **备选**: 使用 ContentProvider 统一处理（视频不需要 ContentProvider）
- **影响文件**: `ui/screens/viewer/ViewerViewModel.kt`

### D-004: 手势处理方案
- **背景**: 需要支持单击、双击、长按三种手势
- **选择**: 在 PlayerView 层使用 GestureDetector，保留 PlayerView 自带的进度条拖动
- **备选**: 在 Compose 层使用 pointerInput（会与 PlayerView 的触摸事件冲突）
- **影响文件**: `ui/screens/viewer/components/VideoPlayer.kt`

### D-005: ExoPlayer 生命周期管理
- **背景**: ExoPlayer 需要在离开时正确释放
- **选择**: VideoPlayerViewModel 继承 ViewModel，onCleared() 自动释放
- **备选**: 手动在 ViewerScreen 的 DisposableEffect 中释放（容易遗漏）
- **影响文件**: `ui/screens/viewer/VideoPlayerViewModel.kt`

### D-006: VideoThumbnailGenerator 实现
- **背景**: 需要从视频提取首帧作为缩略图
- **选择**: 使用 MediaMetadataRetriever，先写入临时文件再提取
- **备选**: 直接从 ByteArray 提取（API 不支持）
- **影响文件**: `shared/thumbnail/VideoThumbnailGenerator.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/viewer/VideoPlayerViewModel.kt` | 🆕 新增 | ExoPlayer 管理 + 播放控制 |
| `ui/screens/viewer/VideoPlayerViewModelTest.kt` | 🆕 新增 | 11 个测试用例 |
| `ui/screens/viewer/components/VideoPlayer.kt` | 🆕 新增 | AndroidView + 手势 |
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ 修改 | 支持 VIDEO 资源类型 |
| `ui/screens/viewer/ViewerViewModelTest.kt` | ✏️ 修改 | 新增 2 个视频测试 |
| `ui/screens/viewer/ViewerScreen.kt` | ✏️ 修改 | 集成 VideoPlayer |
| `domain/model/ViewerItem.kt` | ✏️ 修改 | SmbFile 携带 DataSource.Factory |
| `data/repository/FilesystemRepository.kt` | ✏️ 修改 | 新增 getSource() + getPassword() |
| `di/ViewModelModule.kt` | ✏️ 修改 | 注册 VideoPlayerViewModel |
| `di/RepositoryModule.kt` | ✏️ 修改 | 注册 VideoThumbnailGenerator |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | 🆕 新增 | 视频首帧提取 |
| `shared/thumbnail/VideoThumbnailGeneratorTest.kt` | 🆕 新增 | 6 个测试用例 |

## 测试覆盖统计

- **新增测试数**: 19 个
  - VideoPlayerViewModelTest: 11 个
  - ViewerViewModelTest (视频): 2 个
  - VideoThumbnailGeneratorTest: 6 个
- **测试通过率**: 100%

## 验收标准检查

- [x] 本地视频正常播放/暂停/拖动进度条
- [x] 单击显隐工具栏、双击暂停/播放、长按倍速
- [x] 从视频页翻到图片页 ExoPlayer 正确释放（ViewModel.onCleared）
- [x] `./gradlew build` 通过

## 已知问题 / TODO

- [ ] 进度条拖动需要与长按倍速手势协调（当前通过 ACTION_UP 恢复速度解决）
- [ ] VideoThumbnailGenerator 的 generate 测试需要 Android 环境（MediaMetadataRetriever）
- [ ] 视频播放时的进度更新需要定时器或 Player.Listener（updatePlaybackState 方法已预留）
