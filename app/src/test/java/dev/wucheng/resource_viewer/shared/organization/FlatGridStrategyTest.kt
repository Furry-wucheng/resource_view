package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlatGridStrategyTest {
    private lateinit var strategy: FlatGridStrategy
    private lateinit var mockFileSource: FileSource
    private lateinit var mockResource: Resource

    @Before
    fun setup() {
        strategy = FlatGridStrategy()
        mockFileSource = mockk(relaxed = true)
        mockResource = mockk(relaxed = true)
    }

    @Test
    fun `mode should be FLATGRID`() {
        assertEquals(OrganizationMode.FLATGRID, strategy.mode)
    }

    @Test
    fun `getChapters should return empty list`() =
        runTest {
            val chapters = strategy.getChapters(mockResource, mockFileSource)
            assertTrue(chapters.isEmpty())
        }

    @Test
    fun `getContents should return image and video files`() =
        runTest {
            val entries =
                listOf(
                    FileEntry("photo1.jpg", "photo1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
                    FileEntry("photo2.png", "photo2.png", false, 2048, System.currentTimeMillis(), "png"),
                    FileEntry("video.mp4", "video.mp4", false, 8192, System.currentTimeMillis(), "mp4"),
                    FileEntry("document.pdf", "document.pdf", false, 4096, System.currentTimeMillis(), "pdf"),
                    FileEntry("subfolder", "subfolder", true, 0, System.currentTimeMillis(), ""),
                )
            coEvery { mockFileSource.listDirectory(any()) } returns entries

            val contents = strategy.getContents(mockResource, mockFileSource)

            assertEquals(3, contents.size)
            assertEquals(setOf("jpg", "png", "mp4"), contents.map { it.extension }.toSet())
        }

    @Test
    fun `getContents should include video files`() =
        runTest {
            val entries =
                listOf(
                    FileEntry("video.mp4", "video.mp4", false, 8192, System.currentTimeMillis(), "mp4"),
                )
            coEvery { mockFileSource.listDirectory(any()) } returns entries

            val contents = strategy.getContents(mockResource, mockFileSource)

            assertEquals(1, contents.size)
            assertEquals("mp4", contents[0].extension)
        }

    @Test
    fun `getContents should return empty list when no supported files`() =
        runTest {
            val entries =
                listOf(
                    FileEntry("document.pdf", "document.pdf", false, 4096, System.currentTimeMillis(), "pdf"),
                )
            coEvery { mockFileSource.listDirectory(any()) } returns entries

            val contents = strategy.getContents(mockResource, mockFileSource)

            assertTrue(contents.isEmpty())
        }

    @Test
    fun `createProvider should return ImageFolderProvider`() {
        val provider = strategy.createProvider(mockResource, mockFileSource)
        assertNotNull(provider)
        assertEquals(
            "dev.wucheng.resource_viewer.shared.content.ImageFolderProvider",
            provider::class.java.name,
        )
    }
}
