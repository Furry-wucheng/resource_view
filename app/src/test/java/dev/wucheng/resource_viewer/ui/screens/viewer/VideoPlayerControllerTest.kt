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
 * 核心验证：
 * - 加载前先 stop + clearMediaItems，防止切换视频时 NO_MEMORY
 * - 释放（release）后不再操作底层 MediaCodec
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
    fun `should call stop and clearMediaItems when stopping`() {
        controller.stop()

        verify { mockPlayer.stop() }
        verify { mockPlayer.clearMediaItems() }
    }

    @Test
    fun `should not call stop after release`() {
        controller.release()
        clearMocks(mockPlayer)

        controller.stop()

        verify(exactly = 0) { mockPlayer.stop() }
        verify(exactly = 0) { mockPlayer.clearMediaItems() }
    }

    @Test
    fun `should stop before loading local source`() {
        val localSource = VideoMediaSource.LocalFile("/storage/emulated/0/video.mp4")

        controller.loadMedia(localSource)

        verifySequence {
            mockPlayer.stop()
            mockPlayer.clearMediaItems()
            mockPlayer.setMediaItem(any<MediaItem>())
            mockPlayer.prepare()
            mockPlayer.playWhenReady = true
        }
    }

    @Test
    fun `should stop before loading SMB source`() {
        val mockDataSourceFactory = mockk<DataSource.Factory>(relaxed = true)
        val smbSource = VideoMediaSource.SmbFile(
            dataSourceFactory = mockDataSourceFactory,
            relativePath = "videos/movie.mp4",
            fileSize = 1024L * 1024,
        )

        controller.loadMedia(smbSource)

        verifySequence {
            mockPlayer.stop()
            mockPlayer.clearMediaItems()
            mockPlayer.setMediaSource(any())
            mockPlayer.prepare()
            mockPlayer.playWhenReady = true
        }
    }

    @Test
    fun `should stop previous source when switching videos`() {
        val localSource = VideoMediaSource.LocalFile("/storage/video1.mp4")
        val smbSource = VideoMediaSource.SmbFile(
            dataSourceFactory = mockk(relaxed = true),
            relativePath = "videos/movie.mp4",
            fileSize = 1024L * 1024,
        )

        controller.loadMedia(localSource)
        clearMocks(mockPlayer)

        controller.loadMedia(smbSource)

        verifySequence {
            mockPlayer.stop()
            mockPlayer.clearMediaItems()
            mockPlayer.setMediaSource(any())
            mockPlayer.prepare()
            mockPlayer.playWhenReady = true
        }
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
