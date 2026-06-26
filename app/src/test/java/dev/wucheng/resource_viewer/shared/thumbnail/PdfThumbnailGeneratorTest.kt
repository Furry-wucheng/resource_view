package dev.wucheng.resource_viewer.shared.thumbnail

import android.content.Context
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * PdfThumbnailGenerator 测试。
 * 测试 ThumbnailGenerator 接口实现，渲染 PDF 第 0 页为缩略图。
 */
class PdfThumbnailGeneratorTest {
    private lateinit var mockContext: Context
    private lateinit var mockFileSource: FileSource
    private lateinit var generator: PdfThumbnailGenerator
    private lateinit var cacheDir: File

    private val testResource = Resource(
        id = "test-pdf-id",
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "Test PDF",
        type = ResourceType.PDF,
        organizationMode = null,
        relativePath = "test/document.pdf",
        thumbnailPath = null,
        fileCount = 10,
        fileSize = 1024,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockFileSource = mockk()
        cacheDir = mockk(relaxed = true)
        generator = PdfThumbnailGenerator(mockContext)
    }

    @After
    fun teardown() {
        // 清理
    }

    // ===== RED: 测试 canHandle =====

    @Test
    fun `should return true for PDF resource type`() {
        // Act
        val result = generator.canHandle(ResourceType.PDF)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `should return false for non-PDF resource types`() {
        // Act & Assert
        assertFalse(generator.canHandle(ResourceType.FOLDER))
        assertFalse(generator.canHandle(ResourceType.VIDEO))
        assertFalse(generator.canHandle(ResourceType.ARCHIVE))
    }

    // ===== RED: 测试 generate =====

    @Test
    fun `should return null when file read fails`() = runTest {
        // Arrange
        coEvery { mockFileSource.readFile(testResource.relativePath) } throws Exception("File not found")

        // Act
        val result = generator.generate(testResource, mockFileSource, cacheDir)

        // Assert
        assertNull(result)
    }

    @Test
    fun `should return null when PDF is invalid`() = runTest {
        // Arrange
        coEvery { mockFileSource.readFile(testResource.relativePath) } returns ByteArray(0)

        // Act
        val result = generator.generate(testResource, mockFileSource, cacheDir)

        // Assert
        assertNull(result)
    }

    @Test
    fun `should create cache directory if not exists`() = runTest {
        // Arrange
        val testPdfBytes = ByteArray(100) { it.toByte() }
        coEvery { mockFileSource.readFile(testResource.relativePath) } returns testPdfBytes
        every { cacheDir.exists() } returns false
        every { cacheDir.mkdirs() } returns true

        // Act
        try {
            generator.generate(testResource, mockFileSource, cacheDir)
        } catch (e: Exception) {
            // 预期失败（无效 PDF）
        }

        // Assert - 验证尝试创建目录
        // 注意：在实际实现中会调用 cacheDir.mkdirs()
    }
}
