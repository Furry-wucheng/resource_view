package dev.wucheng.resource_viewer.shared.content

import dev.wucheng.resource_viewer.shared.filesource.FileSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveContentProviderTest {
    @Test fun `zip provider counts images and ignores non images`() = runTest {
        val archive = zipBytes(
            "chapter/page2.jpg" to byteArrayOf(2),
            "readme.txt" to byteArrayOf(9),
            "chapter/page1.png" to byteArrayOf(1),
        )
        val provider = ArchiveContentProvider(FakeFileSource(archive), "book.cbz")

        assertEquals(2, provider.pageCount)
        assertEquals("png", provider.getPageExtension(0))
        assertEquals("jpg", provider.getPageExtension(1))
    }

    @Test fun `rar provider reports unsupported archive format`() {
        val result = runCatching {
            ArchiveContentProvider(FakeFileSource(byteArrayOf(1, 2, 3)), "book.rar")
        }

        assertTrue(result.exceptionOrNull() is UnsupportedArchiveFormatException)
    }

    private class FakeFileSource(private val bytes: ByteArray) : FileSource {
        override val sourceId = "source"
        override suspend fun listDirectory(relativePath: String) = emptyList<dev.wucheng.resource_viewer.domain.model.FileEntry>()
        override suspend fun stat(relativePath: String) = null
        override suspend fun readFile(relativePath: String) = bytes
        override suspend fun readRange(relativePath: String, offset: Long, length: Long) = bytes.copyOfRange(offset.toInt(), (offset + length).toInt())
        override fun openInputStream(relativePath: String) = ByteArrayInputStream(bytes)
        override suspend fun testConnection() = true
        override fun disconnect() = Unit
    }

    private companion object {
        fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            return output.toByteArray()
        }
    }
}
