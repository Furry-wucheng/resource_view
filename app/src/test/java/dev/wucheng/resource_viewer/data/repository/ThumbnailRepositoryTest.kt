package dev.wucheng.resource_viewer.data.repository

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailGenerator
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * ThumbnailRepository 单元测试。
 * 测试缩略图生成器的分发逻辑和错误处理。
 */
class ThumbnailRepositoryTest {

    private lateinit var mockVideoGenerator: ThumbnailGenerator
    private lateinit var mockPdfGenerator: ThumbnailGenerator
    private lateinit var repository: ThumbnailRepository
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

    private val pdfResource = Resource(
        id = "pdf-1",
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "document.pdf",
        type = ResourceType.PDF,
        organizationMode = null,
        relativePath = "docs/document.pdf",
        thumbnailPath = null,
        fileCount = 10,
        fileSize = 1024 * 50,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private val folderResource = Resource(
        id = "folder-1",
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "photos",
        type = ResourceType.FOLDER,
        organizationMode = null,
        relativePath = "photos",
        thumbnailPath = null,
        fileCount = 20,
        fileSize = 1024 * 100,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Before
    fun setup() {
        mockVideoGenerator = mockk(relaxed = true)
        mockPdfGenerator = mockk(relaxed = true)
        mockFileSource = mockk(relaxed = true)
        mockCacheDir = mockk(relaxed = true)

        // 设置 canHandle 行为
        every { mockVideoGenerator.canHandle(ResourceType.VIDEO) } returns true
        every { mockVideoGenerator.canHandle(ResourceType.PDF) } returns false
        every { mockVideoGenerator.canHandle(ResourceType.FOLDER) } returns false
        every { mockVideoGenerator.canHandle(ResourceType.ARCHIVE) } returns false

        every { mockPdfGenerator.canHandle(ResourceType.VIDEO) } returns false
        every { mockPdfGenerator.canHandle(ResourceType.PDF) } returns true
        every { mockPdfGenerator.canHandle(ResourceType.FOLDER) } returns false
        every { mockPdfGenerator.canHandle(ResourceType.ARCHIVE) } returns false

        repository = ThumbnailRepository(setOf(mockVideoGenerator, mockPdfGenerator))
    }

    // ===== generateThumbnail =====

    @Test
    fun `generateThumbnail should use video generator for VIDEO resource`() = runTest {
        // Given
        val expectedFile = mockk<File>()
        coEvery { mockVideoGenerator.generate(videoResource, mockFileSource, mockCacheDir) } returns expectedFile

        // When
        val result = repository.generateThumbnail(videoResource, mockFileSource, mockCacheDir)

        // Then
        assertTrue(result is dev.wucheng.resource_viewer.domain.error.Result.Ok)
        assertEquals(expectedFile, (result as dev.wucheng.resource_viewer.domain.error.Result.Ok).value)
        coVerify { mockVideoGenerator.generate(videoResource, mockFileSource, mockCacheDir) }
    }

    @Test
    fun `generateThumbnail should use pdf generator for PDF resource`() = runTest {
        // Given
        val expectedFile = mockk<File>()
        coEvery { mockPdfGenerator.generate(pdfResource, mockFileSource, mockCacheDir) } returns expectedFile

        // When
        val result = repository.generateThumbnail(pdfResource, mockFileSource, mockCacheDir)

        // Then
        assertTrue(result is dev.wucheng.resource_viewer.domain.error.Result.Ok)
        assertEquals(expectedFile, (result as dev.wucheng.resource_viewer.domain.error.Result.Ok).value)
        coVerify { mockPdfGenerator.generate(pdfResource, mockFileSource, mockCacheDir) }
    }

    @Test
    fun `generateThumbnail should return null when no generator matches`() = runTest {
        // When
        val result = repository.generateThumbnail(folderResource, mockFileSource, mockCacheDir)

        // Then
        assertTrue(result is dev.wucheng.resource_viewer.domain.error.Result.Ok)
        assertNull((result as dev.wucheng.resource_viewer.domain.error.Result.Ok).value)
    }

    @Test
    fun `generateThumbnail should return null when generator returns null`() = runTest {
        // Given
        coEvery { mockVideoGenerator.generate(videoResource, mockFileSource, mockCacheDir) } returns null

        // When
        val result = repository.generateThumbnail(videoResource, mockFileSource, mockCacheDir)

        // Then
        assertTrue(result is dev.wucheng.resource_viewer.domain.error.Result.Ok)
        assertNull((result as dev.wucheng.resource_viewer.domain.error.Result.Ok).value)
    }

    @Test
    fun `generateThumbnail should return MediaLoadError when generator throws exception`() = runTest {
        // Given
        coEvery { mockVideoGenerator.generate(videoResource, mockFileSource, mockCacheDir) } throws
                RuntimeException("Generation failed")

        // When
        val result = repository.generateThumbnail(videoResource, mockFileSource, mockCacheDir)

        // Then
        assertTrue(result is dev.wucheng.resource_viewer.domain.error.Result.Err)
        val error = (result as dev.wucheng.resource_viewer.domain.error.Result.Err).error
        assertTrue(error is DomainError.MediaLoadError)
        assertEquals(dev.wucheng.resource_viewer.domain.error.MediaType.VIDEO, (error as DomainError.MediaLoadError).mediaType)
    }

    @Test
    fun `generateThumbnail should return MediaLoadError with PDF type for PDF generator failure`() = runTest {
        // Given
        coEvery { mockPdfGenerator.generate(pdfResource, mockFileSource, mockCacheDir) } throws
                RuntimeException("PDF generation failed")

        // When
        val result = repository.generateThumbnail(pdfResource, mockFileSource, mockCacheDir)

        // Then
        assertTrue(result is dev.wucheng.resource_viewer.domain.error.Result.Err)
        val error = (result as dev.wucheng.resource_viewer.domain.error.Result.Err).error
        assertTrue(error is DomainError.MediaLoadError)
        assertEquals(dev.wucheng.resource_viewer.domain.error.MediaType.PDF, (error as DomainError.MediaLoadError).mediaType)
    }

    // ===== hasGenerator =====

    @Test
    fun `hasGenerator should return true for VIDEO type`() {
        assertTrue(repository.hasGenerator(ResourceType.VIDEO))
    }

    @Test
    fun `hasGenerator should return true for PDF type`() {
        assertTrue(repository.hasGenerator(ResourceType.PDF))
    }

    @Test
    fun `hasGenerator should return false for FOLDER type`() {
        assertFalse(repository.hasGenerator(ResourceType.FOLDER))
    }

    @Test
    fun `hasGenerator should return false for ARCHIVE type`() {
        assertFalse(repository.hasGenerator(ResourceType.ARCHIVE))
    }

    @Test
    fun `hasGenerator should return false when no generators are registered`() {
        // Given
        val emptyRepository = ThumbnailRepository(emptySet())

        // Then
        assertFalse(emptyRepository.hasGenerator(ResourceType.VIDEO))
        assertFalse(emptyRepository.hasGenerator(ResourceType.PDF))
    }
}
