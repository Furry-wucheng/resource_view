package dev.wucheng.resource_viewer.shared.content

import android.net.Uri
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class MixedFolderProviderTest {
    private lateinit var mockFileSource: FileSource
    private val testRelativePath = "test/folder"
    private val testSourceId = "test-source"

    @Before
    fun setup() {
        mockFileSource = mockk()
    }

    @Test
    fun `buildViewerItems should create ImagePage with extension for images`() = runTest {
        val entries = listOf(
            createEntry("image.gif", false),
            createEntry("video.mp4", false),
            createEntry("photo.jpg", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns entries
        every { mockFileSource.sourceId } returns testSourceId

        val provider = MixedFolderProvider(mockFileSource, testRelativePath, testSourceId)

        val items = provider.buildViewerItems()

        // sorted by relativePath: image.gif, photo.jpg, video.mp4
        assertEquals(3, items.size)
        val first = items[0] as ViewerItem.ImagePage
        assertEquals("gif", first.extension.lowercase())
        assertEquals(0, first.pageIndex)
        assertEquals("mixed:$testSourceId:$testRelativePath", first.providerKey)
        val second = items[1] as ViewerItem.ImagePage
        assertEquals("jpg", second.extension.lowercase())
        assertEquals(1, second.pageIndex)
        val third = items[2] as ViewerItem.Video
        assertEquals("video.mp4", third.title)
    }

    @Test
    fun `getPageExtension should return correct extension`() = runTest {
        val entries = listOf(
            createEntry("animation.gif", false),
            createEntry("photo.jpg", false),
        )
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns entries
        every { mockFileSource.sourceId } returns testSourceId

        val provider = MixedFolderProvider(mockFileSource, testRelativePath, testSourceId)

        assertEquals("gif", provider.getPageExtension(0))
        assertEquals("jpg", provider.getPageExtension(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPageExtension should throw for invalid index`() = runTest {
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns emptyList()
        val provider = MixedFolderProvider(mockFileSource, testRelativePath, testSourceId)
        provider.getPageExtension(0)
    }

    @Test
    fun `getPageUri should cache remote file to local and return Uri`() = runTest {
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        every { mockUri.path } returns "/cache/test.gif"
        every { Uri.fromFile(any()) } returns mockUri

        val entries = listOf(createEntry("animation.gif", false))
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns entries
        coEvery { mockFileSource.openInputStream(any()) } returns ByteArrayInputStream("GIF87a".toByteArray())
        every { mockFileSource.sourceId } returns testSourceId

        val cacheDir = java.io.File.createTempFile("cache", "").apply { delete() }
        val provider = MixedFolderProvider(mockFileSource, testRelativePath, testSourceId, pageCacheDirectory = cacheDir)

        try {
            val uri = provider.getPageUri(0)

            assertNotNull(uri)
            assertTrue(uri.path?.endsWith(".gif") == true)
        } finally {
            unmockkStatic(Uri::class)
            cacheDir.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPageUri should throw for invalid index`() = runTest {
        coEvery { mockFileSource.listDirectory(testRelativePath) } returns emptyList()
        val provider = MixedFolderProvider(mockFileSource, testRelativePath, testSourceId)
        provider.getPageUri(0)
    }

    private fun createEntry(name: String, isDirectory: Boolean): FileEntry {
        return FileEntry(
            name = name,
            relativePath = "$testRelativePath/$name",
            isDirectory = isDirectory,
            size = if (isDirectory) 0L else 1024L,
            modifiedAt = System.currentTimeMillis(),
            extension = name.substringAfterLast('.', ""),
        )
    }
}
