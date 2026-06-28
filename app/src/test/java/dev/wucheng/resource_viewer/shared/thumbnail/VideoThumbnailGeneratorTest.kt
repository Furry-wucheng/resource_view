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
 * 注意：MediaMetadataRetriever 是 Android API，需要 mock 或借助 Robolectric。
 * 此测试验证 canHandle 逻辑、generate 调用流程，以及不再调用 readFile（全量读取）。
 */
class VideoThumbnailGeneratorTest {

    private lateinit var generator: VideoThumbnailGenerator
    private lateinit var mockFileSource: FileSource

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
        generator = VideoThumbnailGenerator(null)
        mockFileSource = mockk(relaxed = true)
    }

    // ===== canHandle 测试 =====

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

    // ===== generate 流程测试 =====

    @Test
    fun `generate should not call readFile on fileSource`() = runTest {
        // Given
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-cache-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        // When — generate 会因为 MediaMetadataRetriever 不可用而返回 null
        // 但关键是验证它**没有**调用 readFile
        generator.generate(videoResource, mockFileSource, tempDir)

        // Then
        coVerify(exactly = 0) { mockFileSource.readFile(any()) }
    }

    @Test
    fun `generate should return null when MediaMetadataRetriever fails`() = runTest {
        // Given
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-cache-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        // When — 没有 mock 真实的 MediaMetadataRetriever，返回 null
        val result = generator.generate(videoResource, mockFileSource, tempDir)

        // Then
        assertNull(result)
    }

    @Test
    fun `generate should return cached file when it already exists`() = runTest {
        // Given
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-cache-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        // 预创建缓存文件
        val cachedFile = File(tempDir, "thumb_${videoResource.id}.jpg")
        cachedFile.writeBytes("fake-image".toByteArray())

        // When
        val result = generator.generate(videoResource, mockFileSource, tempDir)

        // Then
        assertNotNull(result)
        assertEquals(cachedFile.absolutePath, result!!.absolutePath)
    }
}
