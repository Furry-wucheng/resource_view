# M19 — 视频播放器组件

> 轨道 6 · Stage 19/29 | 前置: M11,M14 | 依赖共享: `doc/share/02-interfaces.md` `doc/share/01-data-models.md` §4 | 🟢 独占

## 执行目标

实现 Media3 ExoPlayer 视频播放器 Composable，集成到 ViewerScreen 中。

## 共享契约引用

- `doc/share/01-data-models.md` §4 — ViewerItem.Video / VideoMediaSource
- `@prd/04-资源查看器.md` §4.4 — 视频播放器交互
- `@design/viewer.html` — 原型参考

## 子任务

### M19.1 VideoPlayer Composable

实现视频播放器组件：
- `AndroidView` 包装 ExoPlayer `PlayerView`
- 本地视频：`MediaItem.fromUri(localPath)`
- SMB 视频：`MediaItem` + 自定义 `SmbDataSourceFactory`
- 单击显隐工具栏（与 ViewerScreen 手势统一）
- 双击暂停/播放
- 长按倍速播放

**产出物**：`ui/screens/viewer/components/VideoPlayer.kt`

### M19.2 集成到 ViewerScreen

在 `ViewerScreen` 的 `HorizontalPager` 中处理 `ViewerItem.Video` 分支，渲染 `VideoPlayer`。

**产出物**：`ui/screens/viewer/ViewerScreen.kt`（修改，追加 Video 分支）

### M19.3 ExoPlayer 生命周期

在 `ViewerViewModel.onCleared()` 中释放 ExoPlayer。

## 验收标准

- [ ] 本地视频正常播放/暂停/拖动进度条
- [ ] 单击显隐工具栏、双击暂停/播放、长按倍速
- [ ] 从视频页翻到图片页 ExoPlayer 正确释放
- [ ] `./gradlew build` 通过
