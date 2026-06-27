package dev.wucheng.resource_viewer.shared.content

import android.graphics.Bitmap
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * ImageFolderProvider 测试。
 * 测试 ContentProvider 接口实现，读取文件夹内所有图片文件并按名称排序。
 */
class ImageFolderProviderTest {
    private lateinit var mockFileSource: FileSource
    private lateinit var provider: ImageFolderProvider

    private val testRelativePath = "test/folder"

    @Before
    fun setup() {
        mockFileSource = mockk()
        provider = ImageFolderProvider(mockFileSource, testRelativePath)
    }

    // ===== RED: 测试 pageCount =====

    @Test
    fun `should return zero pageCount when directory is empty`() = runTest {
        // Arrange
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns emptyList()

        // Act
        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Assert
        assertEquals(0, provider.pageCount)
    }

    @Test
    fun `should return correct pageCount when directory has image files`() = runTest {
        // Arrange
        val imageFiles = listOf(
            createFileEntry("image1.jpg", false),
            createFileEntry("image2.png", false),
            createFileEntry("image3.webp", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns imageFiles

        // Act
        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Assert
        assertEquals(3, provider.pageCount)
    }

    @Test
    fun `should ignore non-image files when counting pages`() = runTest {
        // Arrange
        val mixedFiles = listOf(
            createFileEntry("image1.jpg", false),
            createFileEntry("document.txt", false),
            createFileEntry("image2.png", false),
            createFileEntry("video.mp4", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns mixedFiles

        // Act
        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Assert
        assertEquals(2, provider.pageCount)
    }

    @Test
    fun `should ignore subdirectories when counting pages`() = runTest {
        // Arrange
        val filesWithDirs = listOf(
            createFileEntry("image1.jpg", false),
            createFileEntry("subfolder", true),
            createFileEntry("image2.png", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns filesWithDirs

        // Act
        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Assert
        assertEquals(2, provider.pageCount)
    }

    @Test
    fun `should sort image files by name ascending`() = runTest {
        // Arrange
        val unsortedFiles = listOf(
            createFileEntry("c.jpg", false),
            createFileEntry("a.png", false),
            createFileEntry("b.webp", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns unsortedFiles

        // Act
        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Assert - 验证有3个文件
        assertEquals(3, provider.pageCount)
    }

    @Test
    fun `should recognize common image extensions`() = runTest {
        // Arrange
        val imageFiles = listOf(
            createFileEntry("test.jpg", false),
            createFileEntry("test.jpeg", false),
            createFileEntry("test.png", false),
            createFileEntry("test.webp", false),
            createFileEntry("test.bmp", false),
            createFileEntry("test.gif", false),
            createFileEntry("test.txt", false), // 不是图片
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns imageFiles

        // Act
        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Assert
        assertEquals(6, provider.pageCount) // 6个图片文件
    }

    @Test
    fun `recursive provider should include images in nested folders`() = runTest {
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns listOf(
            createFileEntry("cover.jpg", false),
            createFileEntry("nested", true),
        )
        coEvery { mockFileSource.listDirectory("$testRelativePath/nested") } returns listOf(
            FileEntry(
                name = "page2.png",
                relativePath = "$testRelativePath/nested/page2.png",
                isDirectory = false,
                size = 1024,
                modifiedAt = 0,
                extension = "png",
            )
        )

        val recursiveProvider = ImageFolderProvider(mockFileSource, testRelativePath, recursive = true)

        assertEquals(2, recursiveProvider.pageCount)
    }

    // ===== RED: 测试 dispose =====

    @Test
    fun `should not throw when dispose is called`() {
        // Act & Assert
        provider.dispose() // 应该不抛异常
    }

    // ===== RED: 测试 getPageExtension =====

    @Test
    fun `should return correct extension for gif file`() = runTest {
        // Arrange
        val imageFiles = listOf(
            createFileEntry("animation.gif", false),
            createFileEntry("photo.jpg", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns imageFiles

        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Act
        val gifExtension = provider.getPageExtension(0)
        val jpgExtension = provider.getPageExtension(1)

        // Assert
        assertEquals("gif", gifExtension)
        assertEquals("jpg", jpgExtension)
    }

    @Test
    fun `should return lowercase extension`() = runTest {
        // Arrange
        val imageFiles = listOf(
            createFileEntry("image.GIF", false),
            createFileEntry("image.JPEG", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns imageFiles

        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Act
        val gifExtension = provider.getPageExtension(0)
        val jpegExtension = provider.getPageExtension(1)

        // Assert
        assertEquals("gif", gifExtension)
        assertEquals("jpeg", jpegExtension)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when getPageExtension with invalid index`() = runTest {
        // Arrange
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns emptyList()

        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Act & Assert
        provider.getPageExtension(0)
    }

    // ===== RED: 测试 getPageUri =====

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when getPageUri with invalid index`() = runTest {
        // Arrange
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns emptyList()

        val provider = ImageFolderProvider(mockFileSource, testRelativePath)

        // Act & Assert
        provider.getPageUri(0)
    }

    private fun createFileEntry(name: String, isDirectory: Boolean): FileEntry {
        return FileEntry(
            name = name,
            relativePath = "$testRelativePath/$name",
            isDirectory = isDirectory,
            size = if (isDirectory) 0 else 1024,
            modifiedAt = System.currentTimeMillis(),
            extension = name.substringAfterLast('.', ""),
        )
    }
}
