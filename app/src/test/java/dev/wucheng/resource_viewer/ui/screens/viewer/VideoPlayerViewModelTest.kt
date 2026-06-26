package dev.wucheng.resource_viewer.ui.screens.viewer

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VideoPlayerViewModel 单元测试。
 * 使用 MockK mock ExoPlayer 进行纯 JVM 测试。
 *
 * 注意：此实现遵循 doc/mvp/M19-video-player.md 中的 M19.1 子任务。
 */
class VideoPlayerViewModelTest {

    private lateinit var mockPlayer: ExoPlayer
    private lateinit var viewModel: VideoPlayerViewModel

    @Before
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        viewModel = VideoPlayerViewModel(mockPlayer)

        // Mock android.net.Uri for JVM unit tests
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    @After
    fun teardown() {
        viewModel.release()
        unmockkStatic(Uri::class)
    }

    // ===== RED: 加载本地视频 =====

    @Test
    fun `should set media item when loadMedia called with local source`() {
        // Given
        val localSource = VideoMediaSource.LocalFile("/storage/emulated/0/video.mp4")

        // When
        viewModel.loadMedia(localSource)

        // Then
        verify { mockPlayer.setMediaItem(any<MediaItem>()) }
        verify { mockPlayer.prepare() }
        verify { mockPlayer.playWhenReady = true }
    }

    // ===== RED: 加载 SMB 视频 =====

    @Test
    fun `should set media source when loadMedia called with SMB source`() {
        // Given
        val mockDataSourceFactory = mockk<DataSource.Factory>(relaxed = true)
        val smbSource = VideoMediaSource.SmbFile(
            dataSourceFactory = mockDataSourceFactory,
            relativePath = "videos/movie.mp4",
            fileSize = 1024L * 1024,
        )

        // When
        viewModel.loadMedia(smbSource)

        // Then
        verify { mockPlayer.setMediaSource(any()) }
        verify { mockPlayer.prepare() }
        verify { mockPlayer.playWhenReady = true }
    }

    // ===== RED: 播放/暂停切换 =====

    @Test
    fun `should play when togglePlayPause and player is not playing`() {
        // Given
        every { mockPlayer.isPlaying } returns false

        // When
        viewModel.togglePlayPause()

        // Then
        verify { mockPlayer.play() }
    }

    @Test
    fun `should pause when togglePlayPause and player is playing`() {
        // Given
        every { mockPlayer.isPlaying } returns true

        // When
        viewModel.togglePlayPause()

        // Then
        verify { mockPlayer.pause() }
    }

    // ===== RED: 播放速度 =====

    @Test
    fun `should set playback speed when setPlaybackSpeed called`() {
        // When
        viewModel.setPlaybackSpeed(2.0f)

        // Then
        verify { mockPlayer.setPlaybackSpeed(2.0f) }
        assertEquals(2.0f, viewModel.playbackSpeed.value, 0.01f)
    }

    @Test
    fun `should restore normal speed when restoreNormalSpeed called`() {
        // When
        viewModel.restoreNormalSpeed()

        // Then
        verify { mockPlayer.setPlaybackSpeed(1.0f) }
        assertEquals(1.0f, viewModel.playbackSpeed.value, 0.01f)
    }

    // ===== RED: 释放资源 =====

    @Test
    fun `should release player when release called`() {
        // When
        viewModel.release()

        // Then
        verify { mockPlayer.release() }
    }

    // ===== RED: 状态暴露 =====

    @Test
    fun `should expose isPlaying state from player`() {
        // Given
        every { mockPlayer.isPlaying } returns true

        // When
        viewModel.togglePlayPause()

        // Then
        assertTrue(viewModel.isPlaying.value)
    }

    @Test
    fun `should expose playback speed after set`() {
        // When
        viewModel.setPlaybackSpeed(1.5f)

        // Then
        assertEquals(1.5f, viewModel.playbackSpeed.value, 0.01f)
    }

    // ===== RED: 跳转 =====

    @Test
    fun `should seek to position when seekTo called`() {
        // When
        viewModel.seekTo(30000L)

        // Then
        verify { mockPlayer.seekTo(30000L) }
        assertEquals(30000L, viewModel.currentPositionMs.value)
    }

    // ===== RED: 更新播放状态 =====

    @Test
    fun `should update playback state from player`() {
        // Given
        every { mockPlayer.isPlaying } returns true
        every { mockPlayer.currentPosition } returns 5000L
        every { mockPlayer.duration } returns 120000L
        every { mockPlayer.playbackParameters } returns PlaybackParameters(1.5f)

        // When
        viewModel.updatePlaybackState()

        // Then
        assertTrue(viewModel.isPlaying.value)
        assertEquals(5000L, viewModel.currentPositionMs.value)
        assertEquals(120000L, viewModel.durationMs.value)
        assertEquals(1.5f, viewModel.playbackSpeed.value, 0.01f)
    }
}
