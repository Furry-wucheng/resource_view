package dev.wucheng.resource_viewer.ui.screens.viewer.components

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import dev.wucheng.resource_viewer.ui.screens.viewer.VideoPlayerViewModel

/**
 * 视频播放器 Composable。
 * 使用 [AndroidView] 包装 ExoPlayer [PlayerView]，处理手势交互。
 *
 * 手势：
 * - 单击：显隐工具栏（委托给 [onToggleToolbar]）
 * - 双击：暂停/播放
 * - 长按：倍速播放（松手恢复正常速度）
 *
 * 注意：此实现遵循 doc/mvp/M19-video-player.md 中的 M19.1 子任务。
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    viewModel: VideoPlayerViewModel,
    onToggleToolbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 使用 AndroidView 包装 PlayerView，并通过 GestureDetector 处理手势
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = viewModel.exoPlayer
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                // 设置手势检测器
                val gestureDetector = GestureDetector(
                    ctx,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            onToggleToolbar()
                            return true
                        }

                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            viewModel.togglePlayPause()
                            return true
                        }

                        override fun onLongPress(e: MotionEvent) {
                            // 长按开始：2x 倍速
                            viewModel.setPlaybackSpeed(2.0f)
                        }
                    },
                )

                // 在 PlayerView 的触摸事件中分发手势
                setOnTouchListener { v, event ->
                    gestureDetector.onTouchEvent(event)
                    // 长按松开时恢复正常速度
                    if (event.action == MotionEvent.ACTION_UP ||
                        event.action == MotionEvent.ACTION_CANCEL
                    ) {
                        viewModel.restoreNormalSpeed()
                    }
                    false // 不消费事件，让 PlayerView 也处理（如进度条拖动）
                }
            }
        },
        modifier = modifier,
    )

    // 在离开组合时暂停播放并恢复正常速度
    DisposableEffect(Unit) {
        onDispose {
            viewModel.restoreNormalSpeed()
        }
    }
}
