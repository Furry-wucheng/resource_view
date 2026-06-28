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
 * VideoPlayerController 单元测试。
 * 使用 MockK mock ExoPlayer 进行纯 JVM 测试。
 *
 * 核心验证：释放（release）后不再操作底层 MediaCodec，避免 NO_MEMORY 的连锁错误。
 */
class VideoPlayerControllerTest {

    private lateinit var mockPlayer: ExoPlayer
    private lateinit var controller: VideoPlayerController

    @Before
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        controller = VideoPlayerController(mockPlayer)

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    @After
    fun teardown() {
        controller.release()
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should set media item when loadMedia called with local source`() {
        val localSource = VideoMediaSource.LocalFile("/storage/emulated/0/video.mp4")

        controller.loadMedia(localSource)

        verify { mockPlayer.setMediaItem(any<MediaItem>()) }
        verify { mockPlayer.prepare() }
        verify { mockPlayer.playWhenReady = true }
    }

    @Test
    fun `should set media source when loadMedia called with SMB source`() {
        val mockDataSourceFactory = mockk<DataSource.Factory>(relaxed = true)
        val smbSource = VideoMediaSource.SmbFile(
            dataSourceFactory = mockDataSourceFactory,
            relativePath = "videos/movie.mp4",
            fileSize = 1024L * 1024,
        )

        controller.loadMedia(smbSource)

        verify { mockPlayer.setMediaSource(any()) }
        verify { mockPlayer.prepare() }
        verify { mockPlayer.playWhenReady = true }
    }

    @Test
    fun `should play when togglePlayPause and player is not playing`() {
        every { mockPlayer.isPlaying } returns false

        controller.togglePlayPause()

        verify { mockPlayer.play() }
    }

    @Test
    fun `should pause when togglePlayPause and player is playing`() {
        every { mockPlayer.isPlaying } returns true

        controller.togglePlayPause()

        verify { mockPlayer.pause() }
    }

    @Test
    fun `should set playback speed when setPlaybackSpeed called`() {
        controller.setPlaybackSpeed(2.0f)

        verify { mockPlayer.setPlaybackSpeed(2.0f) }
        assertEquals(2.0f, controller.playbackSpeed.value, 0.01f)
    }

    @Test
    fun `should restore normal speed when restoreNormalSpeed called`() {
        controller.restoreNormalSpeed()

        verify { mockPlayer.setPlaybackSpeed(1.0f) }
        assertEquals(1.0f, controller.playbackSpeed.value, 0.01f)
    }

    @Test
    fun `should release player when release called`() {
        controller.release()

        verify { mockPlayer.release() }
    }

    @Test
    fun `should expose isPlaying state from player`() {
        every { mockPlayer.isPlaying } returns true

        controller.togglePlayPause()

        assertTrue(controller.isPlaying.value)
    }

    @Test
    fun `should expose playback speed after set`() {
        controller.setPlaybackSpeed(1.5f)

        assertEquals(1.5f, controller.playbackSpeed.value, 0.01f)
    }

    @Test
    fun `should seek to position when seekTo called`() {
        controller.seekTo(30000L)

        verify { mockPlayer.seekTo(30000L) }
        assertEquals(30000L, controller.currentPositionMs.value)
    }

    @Test
    fun `should update playback state from player`() {
        every { mockPlayer.isPlaying } returns true
        every { mockPlayer.currentPosition } returns 5000L
        every { mockPlayer.duration } returns 120000L
        every { mockPlayer.playbackParameters } returns PlaybackParameters(1.5f)

        controller.updatePlaybackState()

        assertTrue(controller.isPlaying.value)
        assertEquals(5000L, controller.currentPositionMs.value)
        assertEquals(120000L, controller.durationMs.value)
        assertEquals(1.5f, controller.playbackSpeed.value, 0.01f)
    }

    @Test
    fun `should not operate player after release`() {
        controller.release()
        clearMocks(mockPlayer)

        // 释放后再次调用 loadMedia 不应操作 player
        val localSource = VideoMediaSource.LocalFile("/storage/emulated/0/video.mp4")
        controller.loadMedia(localSource)
        verify(exactly = 0) { mockPlayer.setMediaItem(any()) }
        verify(exactly = 0) { mockPlayer.prepare() }

        // togglePlayPause 不应操作 player
        controller.togglePlayPause()
        verify(exactly = 0) { mockPlayer.play() }
        verify(exactly = 0) { mockPlayer.pause() }

        // seekTo 不应操作 player
        controller.seekTo(10000L)
        verify(exactly = 0) { mockPlayer.seekTo(any()) }
    }
}
