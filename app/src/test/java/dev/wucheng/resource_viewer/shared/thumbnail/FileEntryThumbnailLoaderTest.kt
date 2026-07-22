package dev.wucheng.resource_viewer.shared.thumbnail

import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream

class FileEntryThumbnailLoaderTest {
    @Test fun `browser folder preview only searches direct children`() = runTest {
        val source = FakeFileSource(
            mapOf(
                "root" to listOf(directory("nested", "root/nested")),
                "root/nested" to listOf(image("cover.avif", "root/nested/cover.avif")),
            ),
        )
        val loader = FileEntryThumbnailLoader(source)

        assertNull(loader.findPreviewEntry(directory("root", "root"), ThumbnailSearchPolicy.DIRECT_CHILD))
    }

    @Test fun `resource cover may search nested directories within limits`() = runTest {
        val source = FakeFileSource(
            mapOf(
                "root" to listOf(directory("nested", "root/nested")),
                "root/nested" to listOf(image("cover.avif", "root/nested/cover.avif")),
            ),
        )
        val loader = FileEntryThumbnailLoader(source)

        assertEquals(
            "root/nested/cover.avif",
            loader.findPreviewEntry(directory("root", "root"), ThumbnailSearchPolicy.RESOURCE_COVER)?.relativePath,
        )
    }

    @Test fun `browser preview should treat pdf file as previewable`() = runTest {
        val source = FakeFileSource(emptyMap())
        val loader = FileEntryThumbnailLoader(source)

        assertEquals(
            "root/book.pdf",
            loader.findPreviewEntry(pdf("book.pdf", "root/book.pdf"), ThumbnailSearchPolicy.DIRECT_CHILD)?.relativePath,
        )
    }

    @Test fun `browser folder preview should prefer direct pdf after images and videos`() = runTest {
        val source = FakeFileSource(
            mapOf(
                "root" to listOf(
                    pdf("book.pdf", "root/book.pdf"),
                    text("readme.txt", "root/readme.txt"),
                ),
            ),
        )
        val loader = FileEntryThumbnailLoader(source)

        assertEquals(
            "root/book.pdf",
            loader.findPreviewEntry(directory("root", "root"), ThumbnailSearchPolicy.DIRECT_CHILD)?.relativePath,
        )
    }

    @Test fun `browser preview should treat archive file as previewable`() = runTest {
        val source = FakeFileSource(emptyMap())
        val loader = FileEntryThumbnailLoader(source)

        assertEquals(
            "root/book.cbz",
            loader.findPreviewEntry(archive("book.cbz", "root/book.cbz"), ThumbnailSearchPolicy.DIRECT_CHILD)?.relativePath,
        )
    }

    @Test fun `archive thumbnail should not read file when archive exceeds memory limit`() = runTest {
        val source = FakeFileSource(emptyMap())
        val loader = FileEntryThumbnailLoader(source)

        assertNull(
            loader.load(
                archive("large.cbz", "root/large.cbz", 33L * 1024 * 1024),
                policy = ThumbnailSearchPolicy.DIRECT_CHILD,
            ),
        )
        assertFalse(source.wasFileRead)
    }

    private class FakeFileSource(private val directories: Map<String, List<FileEntry>>) : FileSource {
        override val sourceId = "test"
        var wasFileRead = false
            private set
        override suspend fun listDirectory(relativePath: String) = directories[relativePath].orEmpty()
        override suspend fun stat(relativePath: String) = null
        override suspend fun readFile(relativePath: String): ByteArray {
            wasFileRead = true
            return ByteArray(0)
        }
        override suspend fun readRange(relativePath: String, offset: Long, length: Long) = ByteArray(0)
        override fun openInputStream(relativePath: String) = ByteArrayInputStream(ByteArray(0))
        override suspend fun testConnection() = true
        override fun disconnect() = Unit
    }

    private companion object {
        fun directory(name: String, path: String) = FileEntry(name, path, true, 0, 0)
        fun image(name: String, path: String) = FileEntry(name, path, false, 10, 0, name.substringAfterLast('.'))
        fun pdf(name: String, path: String) = FileEntry(name, path, false, 10, 0, "pdf")
        fun archive(name: String, path: String, size: Long = 10) =
            FileEntry(name, path, false, size, 0, name.substringAfterLast('.'))
        fun text(name: String, path: String) = FileEntry(name, path, false, 10, 0, "txt")
    }
}
