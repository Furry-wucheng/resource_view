package dev.wucheng.resource_viewer.ui.screens.viewer.components

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import dev.wucheng.resource_viewer.ui.screens.viewer.VideoPlayerViewModel
import kotlin.math.absoluteValue

/**
 * 视频播放器 Composable。
 * 使用 [AndroidView] 包装 ExoPlayer [PlayerView]，处理手势交互。
 *
 * 手势：
 * - 单击：显隐工具栏（委托给 [onToggleToolbar]）
 * - 双击：暂停/播放
 * - 长按：倍速播放（松手恢复正常速度）
 * - 底部区域水平拖动：seek（快进/快退）
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
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
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
            modifier = Modifier.fillMaxSize(),
        )

        // 底部 seek 手势热区（屏幕下方 20% 区域）
        VideoSeekGestureArea(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onSeek = { viewModel.seekTo(it) },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.2f)
                .align(Alignment.BottomCenter),
        )

        // 在离开组合时暂停播放并恢复正常速度
        DisposableEffect(Unit) {
            onDispose {
                viewModel.restoreNormalSpeed()
            }
        }
    }
}

/**
 * 视频 seek 手势热区。
 * 在底部区域水平拖动 → 快进/快退。
 * 拖动时显示预览时间，松手后跳转。
 */
@Composable
private fun VideoSeekGestureArea(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var seeking by remember { mutableStateOf(false) }
    var seekTargetMs by remember { mutableLongStateOf(0L) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.pointerInput(durationMs) {
            detectHorizontalDragGestures(
                onDragStart = {
                    seeking = true
                    seekTargetMs = currentPositionMs
                    dragAccumulator = 0f
                },
                onDragEnd = {
                    if (seeking) {
                        onSeek(seekTargetMs.coerceIn(0L, durationMs))
                        seeking = false
                    }
                },
                onDragCancel = {
                    seeking = false
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (durationMs <= 0) return@detectHorizontalDragGestures
                    dragAccumulator += dragAmount
                    // 每像素对应 duration 的 0.1%
                    val msPerPx = durationMs * 0.001f
                    val deltaMs = (dragAccumulator * msPerPx).toLong()
                    if (deltaMs != 0L) {
                        seekTargetMs = (seekTargetMs + deltaMs).coerceIn(0L, durationMs)
                        dragAccumulator -= deltaMs / msPerPx
                    }
                },
            )
        },
    ) {
        // 拖动时显示预览时间
        if (seeking && durationMs > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${formatTime(seekTargetMs)} / ${formatTime(durationMs)}",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
