package dev.wucheng.resource_viewer.ui.screens.viewer.components

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import dev.wucheng.resource_viewer.R
import dev.wucheng.resource_viewer.ui.screens.viewer.VideoPlayerController
import kotlin.math.round

private val PanelBg = Color.Black.copy(alpha = 0.65f)
private val BottomBarShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    controller: VideoPlayerController,
    toolbarVisible: Boolean,
    onToggleToolbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by controller.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by controller.durationMs.collectAsStateWithLifecycle()

    var isLongPressing by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(2f) }
    var longPressStartY by remember { mutableFloatStateOf(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var dragTargetMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(200)
            controller.updatePlaybackState()
        }
    }

    Box(modifier = modifier) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.custom_player_view, null)
                val playerView = view.findViewById<PlayerView>(R.id.custom_player_view)

                playerView.apply {
                    player = controller.exoPlayer
                    useController = false

                    val gestureDetector = GestureDetector(
                        ctx,
                        object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                onToggleToolbar()
                                return true
                            }

                            override fun onDoubleTap(e: MotionEvent): Boolean {
                                controller.togglePlayPause()
                                return true
                            }

                            override fun onLongPress(e: MotionEvent) {
                                isLongPressing = true
                                longPressStartY = e.y
                                currentSpeed = 2.0f
                                controller.setPlaybackSpeed(currentSpeed)
                            }
                        },
                    )

                    setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)

                        if (isLongPressing && event.action == MotionEvent.ACTION_MOVE) {
                            val deltaY = longPressStartY - event.y
                            val speedDelta = (deltaY / 100f) * 0.25f
                            val newSpeed = (2.0f + speedDelta).coerceIn(1f, 3f)
                            currentSpeed = round(newSpeed / 0.25f) * 0.25f
                            controller.setPlaybackSpeed(currentSpeed)
                        }

                        if (event.action == MotionEvent.ACTION_UP ||
                            event.action == MotionEvent.ACTION_CANCEL
                        ) {
                            if (isLongPressing) {
                                isLongPressing = false
                                controller.restoreNormalSpeed()
                            }
                        }
                        true
                    }
                }
                view
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 中央播放/暂停按钮
        AnimatedVisibility(
            visible = toolbarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }

        // 底部 25% 区域：滑动 seek
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .align(Alignment.BottomCenter)
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragTargetMs = currentPositionMs
                        },
                        onDragEnd = {
                            if (isDragging) {
                                controller.seekTo(dragTargetMs.coerceIn(0L, durationMs))
                                isDragging = false
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (durationMs > 0) {
                                val msPerPx = durationMs * 0.001f
                                val deltaMs = (dragAmount * msPerPx).toLong()
                                dragTargetMs = (dragTargetMs + deltaMs).coerceIn(0L, durationMs)
                            }
                        },
                    )
                },
        ) {
            // 底部进度条 + 时间
            val showControls = toolbarVisible || isDragging

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BottomBarShape)
                        .background(PanelBg)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    val progress = if (durationMs > 0) {
                        val displayMs = if (isDragging) dragTargetMs else currentPositionMs
                        displayMs.toFloat() / durationMs
                    } else 0f

                    // 进度条
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progress)
                                .height(3.dp)
                                .background(Color.White, RoundedCornerShape(2.dp)),
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 时间
                    val displayTimeMs = if (isDragging) dragTargetMs else currentPositionMs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatTime(displayTimeMs),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            // 拖动时在进度条上方显示目标时间
            if (isDragging && durationMs > 0) {
                Text(
                    text = formatTime(dragTargetMs),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-48).dp)
                        .background(PanelBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // 倍速提示
        if (isLongPressing) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp)
                    .background(PanelBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${String.format("%.2f", currentSpeed)}x",
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }
        }

        DisposableEffect(Unit) {
            onDispose { controller.restoreNormalSpeed() }
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
