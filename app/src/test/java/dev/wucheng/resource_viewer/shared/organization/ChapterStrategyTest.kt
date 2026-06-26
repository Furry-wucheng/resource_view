package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
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
 * ChapterStrategy 单元测试。
 * 测试章节组织策略：子文件夹作为章节，选章后只浏览该章内容。
 */
class ChapterStrategyTest {

    private lateinit var strategy: ChapterStrategy
    private lateinit var mockFileSource: FileSource
    private lateinit var mockResource: Resource

    @Before
    fun setup() {
        strategy = ChapterStrategy()
        mockFileSource = mockk(relaxed = true)
        mockResource = mockk(relaxed = true)
    }

    @Test
    fun `mode should be CHAPTER`() {
        assertEquals(OrganizationMode.CHAPTER, strategy.mode)
    }

    @Test
    fun `getChapters should return subdirectories as chapters`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
            FileEntry("chapter2", "photos/chapter2", true, 0, System.currentTimeMillis(), ""),
            FileEntry("readme.txt", "photos/readme.txt", false, 100, System.currentTimeMillis(), "txt"),
        )
        coEvery { mockFileSource.listDirectory(any()) } returns entries
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
    fun `getChapters should count image files in each chapter`() = runTest {
        // Given
        val rootEntries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
            FileEntry("chapter2", "photos/chapter2", true, 0, System.currentTimeMillis(), ""),
        )
        val chapter1Entries = listOf(
            FileEntry("img1.jpg", "photos/chapter1/img1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
            FileEntry("img2.png", "photos/chapter1/img2.png", false, 2048, System.currentTimeMillis(), "png"),
            FileEntry("doc.txt", "photos/chapter1/doc.txt", false, 100, System.currentTimeMillis(), "txt"),
        )
        val chapter2Entries = listOf(
            FileEntry("img3.webp", "photos/chapter2/img3.webp", false, 512, System.currentTimeMillis(), "webp"),
        )

        coEvery { mockFileSource.listDirectory("photos") } returns rootEntries
        coEvery { mockFileSource.listDirectory("photos/chapter1") } returns chapter1Entries
        coEvery { mockFileSource.listDirectory("photos/chapter2") } returns chapter2Entries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val chapters = strategy.getChapters(mockResource, mockFileSource)

        // Then
        assertEquals(2, chapters.size)
        assertEquals(2, chapters[0].fileCount) // 2 image files in chapter1
        assertEquals(1, chapters[1].fileCount) // 1 image file in chapter2
    }

    @Test
    fun `getChapters should set cover path to first image in chapter`() = runTest {
        // Given
        val rootEntries = listOf(
            FileEntry("chapter1", "photos/chapter1", true, 0, System.currentTimeMillis(), ""),
        )
        val chapter1Entries = listOf(
            FileEntry("b.jpg", "photos/chapter1/b.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
            FileEntry("a.png", "photos/chapter1/a.png", false, 2048, System.currentTimeMillis(), "png"),
        )

        coEvery { mockFileSource.listDirectory("photos") } returns rootEntries
        coEvery { mockFileSource.listDirectory("photos/chapter1") } returns chapter1Entries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val chapters = strategy.getChapters(mockResource, mockFileSource)

        // Then
        assertEquals(1, chapters.size)
        assertNotNull(chapters[0].coverPath)
    }

    @Test
    fun `getChapters should return empty list when no subdirectories`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("img1.jpg", "photos/img1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
            FileEntry("img2.png", "photos/img2.png", false, 2048, System.currentTimeMillis(), "png"),
        )
        coEvery { mockFileSource.listDirectory(any()) } returns entries
        coEvery { mockResource.relativePath } returns "photos"

        // When
        val chapters = strategy.getChapters(mockResource, mockFileSource)

        // Then
        assertTrue(chapters.isEmpty())
    }

    @Test
    fun `getContents should return empty list`() = runTest {
        // When
        val contents = strategy.getContents(mockResource, mockFileSource)

        // Then
        assertTrue(contents.isEmpty())
    }

    @Test
    fun `createProvider should return ImageFolderProvider for chapter`() {
        // Given
        val chapter = dev.wucheng.resource_viewer.domain.model.Chapter(
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
