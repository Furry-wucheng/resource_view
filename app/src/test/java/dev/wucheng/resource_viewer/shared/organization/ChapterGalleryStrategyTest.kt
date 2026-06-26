package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.content.ImageFolderProvider
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ChapterGalleryStrategy 单元测试。
 * 测试章节画廊组织策略：根层子文件夹作为章节，章内递归扁平所有图片。
 */
class ChapterGalleryStrategyTest {

    private lateinit var strategy: ChapterGalleryStrategy
    private lateinit var mockFileSource: FileSource
    private lateinit var mockResource: Resource

    @Before
    fun setup() {
        strategy = ChapterGalleryStrategy()
        mockFileSource = mockk(relaxed = true)
        mockResource = mockk(relaxed = true)
    }

    @Test
    fun `mode should be CHAPTER_GALLERY`() {
        assertEquals(OrganizationMode.CHAPTER_GALLERY, strategy.mode)
    }

    @Test
    fun `getChapters should return subdirectories as chapters`() = runTest {
        // Given
        val rootEntries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
            FileEntry("chapter2", "photos/chapter2", true, 0, System.currentTimeMillis(), ""),
            FileEntry("readme.txt", "photos/readme.txt", false, 100, System.currentTimeMillis(), "txt"),
        )
        coEvery { mockFileSource.listDirectory("photos") } returns rootEntries
        coEvery { mockFileSource.listDirectory("photos/chapter1") } returns emptyList()
        coEvery { mockFileSource.listDirectory("photos/chapter2") } returns emptyList()
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val chapters = strategy.getChapters(mockResource, mockFileSource)

        // Then
        assertEquals(2, chapters.size)
        assertEquals("chapter1", chapters[0].name)
        assertEquals("photos/chapter1", chapters[0].relativePath)
        assertEquals("chapter2", chapters[1].name)
        assertEquals("photos/chapter2", chapters[1].relativePath)
    }

    @Test
    fun `getChapters should count all image files recursively`() = runTest {
        // Given
        val rootEntries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
        )
        val chapter1Entries = listOf(
            FileEntry("img1.jpg", "photos/chapter1/img1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
            FileEntry("subfolder", "photos/chapter1/subfolder", true, 0, System.currentTimeMillis(), ""),
        )
        val subfolderEntries = listOf(
            FileEntry("img2.png", "photos/chapter1/subfolder/img2.png", false, 2048, System.currentTimeMillis(), "png"),
            FileEntry("img3.webp", "photos/chapter1/subfolder/img3.webp", false, 512, System.currentTimeMillis(), "webp"),
        )

        coEvery { mockFileSource.listDirectory("photos") } returns rootEntries
        coEvery { mockFileSource.listDirectory("photos/chapter1") } returns chapter1Entries
        coEvery { mockFileSource.listDirectory("photos/chapter1/subfolder") } returns subfolderEntries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val chapters = strategy.getChapters(mockResource, mockFileSource)

        // Then
        assertEquals(1, chapters.size)
        assertEquals(3, chapters[0].fileCount) // 3 image files total
    }

    @Test
    fun `getChapters should set cover path to first image found recursively`() = runTest {
        // Given
        val rootEntries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
        )
        val chapter1Entries = listOf(
            FileEntry("subfolder", "photos/chapter1/subfolder", true, 0, System.currentTimeMillis(), ""),
        )
        val subfolderEntries = listOf(
            FileEntry("cover.jpg", "photos/chapter1/subfolder/cover.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
        )

        coEvery { mockFileSource.listDirectory("photos") } returns rootEntries
        coEvery { mockFileSource.listDirectory("photos/chapter1") } returns chapter1Entries
        coEvery { mockFileSource.listDirectory("photos/chapter1/subfolder") } returns subfolderEntries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val chapters = strategy.getChapters(mockResource, mockFileSource)

        // Then
        assertEquals(1, chapters.size)
        assertNotNull(chapters[0].coverPath)
        assertEquals("photos/chapter1/subfolder/cover.jpg", chapters[0].coverPath)
    }

    @Test
    fun `getContents should return all image files recursively from resource`() = runTest {
        // Given
        val rootEntries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
            FileEntry("img0.jpg", "photos/img0.jpg", false, 512, System.currentTimeMillis(), "jpg"),
        )
        val chapter1Entries = listOf(
            FileEntry("img1.jpg", "photos/chapter1/img1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
            FileEntry("subfolder", "photos/chapter1/subfolder", true, 0, System.currentTimeMillis(), ""),
            FileEntry("doc.txt", "photos/chapter1/doc.txt", false, 100, System.currentTimeMillis(), "txt"),
        )
        val subfolderEntries = listOf(
            FileEntry("img2.png", "photos/chapter1/subfolder/img2.png", false, 2048, System.currentTimeMillis(), "png"),
            FileEntry("img3.webp", "photos/chapter1/subfolder/img3.webp", false, 512, System.currentTimeMillis(), "webp"),
        )

        coEvery { mockFileSource.listDirectory("photos") } returns rootEntries
        coEvery { mockFileSource.listDirectory("photos/chapter1") } returns chapter1Entries
        coEvery { mockFileSource.listDirectory("photos/chapter1/subfolder") } returns subfolderEntries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val contents = strategy.getContents(mockResource, mockFileSource)

        // Then
        assertEquals(4, contents.size) // img0 + img1 + img2 + img3
        assertTrue(contents.all { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif") })
    }

    @Test
    fun `getContents should return empty list when no images found`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("doc.txt", "photos/doc.txt", false, 100, System.currentTimeMillis(), "txt"),
            FileEntry("subfolder", "photos/subfolder", true, 0, System.currentTimeMillis(), ""),
        )
        val subfolderEntries = listOf(
            FileEntry("data.csv", "photos/subfolder/data.csv", false, 200, System.currentTimeMillis(), "csv"),
        )

        coEvery { mockFileSource.listDirectory("photos") } returns entries
        coEvery { mockFileSource.listDirectory("photos/subfolder") } returns subfolderEntries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val contents = strategy.getContents(mockResource, mockFileSource)

        // Then
        assertTrue(contents.isEmpty())
    }

    @Test
    fun `createProvider should return ImageFolderProvider for chapter`() {
        // Given
        val chapter = Chapter(
            name = "chapter1",
            relativePath = "photos/chapter1",
            fileCount = 5,
        )

        // When
        val provider = strategy.createProvider(mockResource, mockFileSource, chapter)

        // Then
        assertNotNull(provider)
        assertTrue(provider is ImageFolderProvider)
    }

    @Test
    fun `createProvider should throw when chapter is null`() {
        // When / Then
        try {
            strategy.createProvider(mockResource, mockFileSource, null)
            fail("Should throw IllegalArgumentException when chapter is null")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
