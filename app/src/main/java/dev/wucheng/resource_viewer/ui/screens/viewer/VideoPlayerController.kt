package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 视频播放器控制器。
 * 持有 ExoPlayer 实例，提供播放控制和状态暴露。
 *
 * 与 [VideoPlayerViewModel] 的区别：
 * - 不继承 ViewModel，生命周期由 Composable 通过 [remember] + [DisposableEffect] 显式管理；
 * - 离开视频页面时调用方必须 [release]，底层 MediaCodec 立即释放，防止 NO_MEMORY。
 */
@androidx.media3.common.util.UnstableApi
class VideoPlayerController(
    private val player: ExoPlayer,
) {
    /** ExoPlayer 实例，供 PlayerView 绑定 */
    val exoPlayer: ExoPlayer get() = player

    /** 是否正在播放 */
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** 当前播放速度 */
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    /** 当前播放位置 (ms) */
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    /** 视频总时长 (ms) */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private var isReleased = false

    /**
     * 停止播放并清除当前媒体资源。
     * 释放底层 MediaCodec，确保切换视频时旧解码器已释放。
     */
    fun stop() {
        if (isReleased) return
        player.stop()
        player.clearMediaItems()
    }

    /**
     * 加载视频资源。
     * 根据 [VideoMediaSource] 类型设置不同的 MediaSource。
     *
     * 加载前先调用 [stop] 确保旧解码器已释放，防止切换时 NO_MEMORY。
     *
     * - [VideoMediaSource.LocalFile]: 使用文件路径 URI，ExoPlayer 默认 DataSource 处理
     * - [VideoMediaSource.SmbFile]: 使用调用方提供的 [DataSource.Factory] 构建 ProgressiveMediaSource
     */
    fun loadMedia(source: VideoMediaSource) {
        if (isReleased) return
        stop()
        when (source) {
            is VideoMediaSource.LocalFile -> {
                val mediaItem = MediaItem.fromUri(source.path)
                player.setMediaItem(mediaItem)
            }
            is VideoMediaSource.SmbFile -> {
                val mediaSource = ProgressiveMediaSource.Factory(source.dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(source.relativePath))
                player.setMediaSource(mediaSource)
            }
        }
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * 切换播放/暂停。
     */
    fun togglePlayPause() {
        if (isReleased) return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        _isPlaying.value = player.isPlaying
    }

    fun pause() {
        if (isReleased) return
        player.pause()
        _isPlaying.value = false
    }

    /**
     * 设置播放速度。
     * @param speed 播放速度倍率 (0.5 ~ 3.0)
     */
    fun setPlaybackSpeed(speed: Float) {
        if (isReleased) return
        player.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
        _isPlaying.value = player.isPlaying
    }

    /**
     * 恢复正常播放速度 (1.0x)。
     */
    fun restoreNormalSpeed() {
        setPlaybackSpeed(1.0f)
    }

    /**
     * 跳转到指定位置。
     * @param positionMs 目标位置 (ms)
     */
    fun seekTo(positionMs: Long) {
        if (isReleased) return
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    /**
     * 更新播放状态。
     * 由外部定时调用或监听 Player.Listener。
     */
    fun updatePlaybackState() {
        if (isReleased) return
        _isPlaying.value = player.isPlaying
        _currentPositionMs.value = player.currentPosition
        _durationMs.value = player.duration.coerceAtLeast(0)
        _playbackSpeed.value = player.playbackParameters.speed
    }

    /**
     * 释放 ExoPlayer 资源。
     * 释放后所有操作均会被忽略，防止对已释放的 MediaCodec 二次调用。
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        player.release()
    }
}
