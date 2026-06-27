package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 视频播放器 ViewModel。
 * 管理 ExoPlayer 实例，提供播放控制和状态暴露。
 *
 * 接收外部注入的 [ExoPlayer] 实例（由 Koin 创建），
 * 保证可测试性（单元测试中注入 mock）。
 *
 * 注意：此实现遵循 doc/mvp/M19-video-player.md 中的 M19.1 子任务。
 */
@androidx.media3.common.util.UnstableApi
class VideoPlayerViewModel(
    private val player: ExoPlayer,
) : ViewModel() {
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

    /**
     * 加载视频资源。
     * 根据 [VideoMediaSource] 类型设置不同的 MediaSource。
     *
     * - [VideoMediaSource.LocalFile]: 使用文件路径 URI，ExoPlayer 默认 DataSource 处理
     * - [VideoMediaSource.SmbFile]: 使用调用方提供的 [DataSource.Factory] 构建 ProgressiveMediaSource
     */
    fun loadMedia(source: VideoMediaSource) {
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
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        _isPlaying.value = player.isPlaying
    }

    fun pause() {
        player.pause()
        _isPlaying.value = false
    }

    /**
     * 设置播放速度。
     * @param speed 播放速度倍率 (0.5 ~ 3.0)
     */
    fun setPlaybackSpeed(speed: Float) {
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
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    /**
     * 更新播放状态。
     * 由外部定时调用或监听 Player.Listener。
     */
    fun updatePlaybackState() {
        _isPlaying.value = player.isPlaying
        _currentPositionMs.value = player.currentPosition
        _durationMs.value = player.duration.coerceAtLeast(0)
        _playbackSpeed.value = player.playbackParameters.speed
    }

    /**
     * 释放 ExoPlayer 资源。
     */
    fun release() {
        player.release()
    }

    override fun onCleared() {
        super.onCleared()
        release()
    }
}
