package dev.wucheng.resource_viewer.shared.content

import android.graphics.Bitmap
import dev.wucheng.resource_viewer.shared.filesource.FileSource

class ArchiveContentProvider(
    private val fileSource: FileSource,
    private val relativePath: String,
) : ContentProvider {
    private val archiveBytes: ByteArray
    private val extension: String
    private val entries: List<ArchiveImageEntry>
    private var isDisposed = false

    init {
        archiveBytes = kotlinx.coroutines.runBlocking {
            fileSource.readFile(relativePath)
        }
        extension = relativePath.substringAfterLast('.', "")
        entries = ArchiveImageReader.listImageEntries(archiveBytes, extension)
    }

    override val pageCount: Int
        get() {
            checkNotDisposed()
            return entries.size
        }

    fun getPageExtension(index: Int): String {
        checkNotDisposed()
        require(index in entries.indices) { "Page index $index out of range [0, ${entries.size})" }
        return entries[index].name.substringAfterLast('.', "").lowercase()
    }

    override suspend fun loadPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        checkNotDisposed()
        require(index in entries.indices) { "Page index $index out of range [0, ${entries.size})" }
        require(targetWidth > 0) { "Width must be positive, got $targetWidth" }
        require(targetHeight > 0) { "Height must be positive, got $targetHeight" }

        val imageBytes = ArchiveImageReader.readImageEntry(archiveBytes, extension, entries[index].name)
        return PageBitmapLoader.decodeImageBytes(imageBytes, targetWidth, targetHeight)
            ?: throw IllegalStateException("Failed to decode archive page: ${entries[index].name}")
    }

    override fun dispose() {
        isDisposed = true
    }

    private fun checkNotDisposed() {
        check(!isDisposed) { "ArchiveContentProvider has been disposed" }
    }
}
