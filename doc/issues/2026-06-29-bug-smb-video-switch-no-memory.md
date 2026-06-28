# bug: SMB 视频切换播放时 NO_MEMORY 崩溃 + 查看器无缝切换断裂

> 日期: 2026-06-29 | 类型: bug | 状态: ✅ 已完成

## 现象

1. SMB 视频切换播放时崩溃：
```
MediaCodecRenderer$DecoderInitializationException: Decoder init failed: OMX.qcom.video.decoder.hevc, error 0xfffffff4 (NO_MEMORY)
```
2. 资源库点视频只有单视频 item，无法翻到同目录下的图片/其他视频
3. 资源库点文件夹只显示图片，视频被静默排除
4. MixedFolderProvider 不支持 GALLERY 递归模式

## 复现步骤

1. SMB 源下打开一个有多个视频的文件夹
2. 播放视频 A，滑动到视频 B
3. 崩溃：NO_MEMORY
4. 从资源库点视频 → 只有单个视频，不能翻页
5. 从资源库点 GALLERY 文件夹 → 看不到其中的视频文件

## 期望效果

1. 视频切换不崩溃
2. 资源库点视频时能无缝翻到同目录下所有图片和视频
3. 资源库点文件夹能看到其中所有图片和视频
4. GALLERY 模式下视频可见

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | 三层根因：① `remember(videoItem.videoSource)` 在切换视频时重建 ExoPlayer，旧 player 的 `release()` 异步未完成时新 player 已调用 `prepare()`，两个解码器争抢硬件 HEVC 解码器 → NO_MEMORY；② HorizontalPager 滑动动画期间新旧两页的 VideoPageContent 共存，各持活跃 ExoPlayer；③ loadVideoResource() 只创建单视频 item，loadContentProviderResource() 只用 ImageFolderProvider 排除视频 |
| 修改文件 | VideoPlayerController.kt, ViewerScreen.kt, ViewerViewModel.kt, MixedFolderProvider.kt + 测试 |
| 影响 stage | M19 (视频播放器), M14 (基础查看器), M20 (Gallery/FlatGrid) |

## 执行计划

1. VideoPlayerController +stop() 方法，loadMedia() 开头调 stop() 同步释放旧解码器
2. ViewerScreen VideoPageContent 移除 remember key 复用 ExoPlayer，DisposableEffect key 改 Unit
3. ViewerScreen Pager 传 isPageSelected，非当前页不创建 ExoPlayer
4. ViewerViewModel.loadVideoResource() 改用 MixedFolderProvider 加载父目录
5. ViewerViewModel.loadContentProviderResource() 非 PDF 改用 MixedFolderProvider
6. MixedFolderProvider +recursive 参数支持 GALLERY

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/viewer/VideoPlayerController.kt` | ✏️ | +stop()，loadMedia() 开头调 stop() |
| `ui/screens/viewer/ViewerScreen.kt` | ✏️ | VideoPageContent 复用 ExoPlayer + isPageSelected |
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ | loadVideoResource/loadContentProviderResource 改用 MixedFolderProvider |
| `shared/content/MixedFolderProvider.kt` | ✏️ | +recursive 参数 |
| `ui/screens/viewer/VideoPlayerControllerTest.kt` | ✏️ | +stop/切换/释放后 测试 |
| `ui/screens/viewer/ViewerViewModelTest.kt` | ✏️ | 适配新构造 + mock listDirectory |
