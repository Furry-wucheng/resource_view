package dev.wucheng.resource_viewer.shared.thumbnail

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * VideoThumbnailGenerator 单元测试。
 *
 * 注意：MediaMetadataRetriever 是 Android API，需要 mock。
 * 此测试验证 canHandle 逻辑和 generate 的调用流程。
 */
class VideoThumbnailGeneratorTest {

    private lateinit var generator: VideoThumbnailGenerator
    private lateinit var mockFileSource: FileSource
    private lateinit var mockCacheDir: File

    private val videoResource = Resource(
        id = "video-1",
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "test_video.mp4",
        type = ResourceType.VIDEO,
        organizationMode = null,
        relativePath = "videos/test_video.mp4",
        thumbnailPath = null,
        fileCount = null,
        fileSize = 1024L * 1024 * 100,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Before
    fun setup() {
        generator = VideoThumbnailGenerator()
        mockFileSource = mockk(relaxed = true)
        mockCacheDir = mockk(relaxed = true)
    }

    // ===== RED: canHandle 测试 =====

    @Test
    fun `canHandle should return true for VIDEO resource type`() {
        assertTrue(generator.canHandle(ResourceType.VIDEO))
    }

    @Test
    fun `canHandle should return false for FOLDER resource type`() {
        assertFalse(generator.canHandle(ResourceType.FOLDER))
    }

    @Test
    fun `canHandle should return false for PDF resource type`() {
        assertFalse(generator.canHandle(ResourceType.PDF))
    }

    @Test
    fun `canHandle should return false for ARCHIVE resource type`() {
        assertFalse(generator.canHandle(ResourceType.ARCHIVE))
    }

    // ===== RED: generate 测试 =====

    @Test
    fun `generate should call readFile on fileSource`() = runTest {
        // Given
        coEvery { mockFileSource.readFile(any()) } returns ByteArray(1024)
        every { mockCacheDir.absolutePath } returns "/tmp/cache"

        // When — generate 会因为 MediaMetadataRetriever 不可用而返回 null
        // 但我们可以验证它调用了 readFile
        generator.generate(videoResource, mockFileSource, mockCacheDir)

        // Then
        coVerify { mockFileSource.readFile("videos/test_video.mp4") }
    }

    @Test
    fun `generate should return null when readFile fails`() = runTest {
        // Given
        coEvery { mockFileSource.readFile(any()) } throws RuntimeException("File not found")

        // When
        val result = generator.generate(videoResource, mockFileSource, mockCacheDir)

        // Then
        assertNull(result)
    }
}
