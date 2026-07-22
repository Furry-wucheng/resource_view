package dev.wucheng.resource_viewer.shared.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.util.Log
import dev.wucheng.resource_viewer.data.remote.pdf.PdfRenderer
import dev.wucheng.resource_viewer.shared.content.ArchiveImageReader
import dev.wucheng.resource_viewer.shared.content.MAX_IN_MEMORY_ARCHIVE_THUMBNAIL_BYTES
import dev.wucheng.resource_viewer.shared.content.archiveExtension
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.shared.content.PageBitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

enum class ThumbnailSearchPolicy {
    /** 数据源浏览器：文件夹只查看直接子文件，不继续下钻。 */
    DIRECT_CHILD,
    /** 资源库封面：有限深度搜索，避免深目录无限扫描。 */
    RESOURCE_COVER,
}

class FileEntryThumbnailLoader(
    private val fileSource: FileSource,
    private val context: Context? = null,
) {
    suspend fun load(
        entry: FileEntry,
        targetSize: Int = 320,
        policy: ThumbnailSearchPolicy = ThumbnailSearchPolicy.DIRECT_CHILD,
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val preview = findPreviewEntry(entry, policy) ?: return@withContext null
            when {
                MediaFormats.isImage(preview.extension) -> decodeImage(preview.relativePath, targetSize)
                MediaFormats.isVideo(preview.extension) -> decodeVideo(preview, targetSize)
                isPdf(preview.extension) -> decodePdf(preview.relativePath, targetSize)
                MediaFormats.isArchive(preview.archiveExtension()) -> decodeArchive(preview, targetSize)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail load failed for ${entry.relativePath}", e)
            null
        }
    }

    internal suspend fun findPreviewEntry(entry: FileEntry, policy: ThumbnailSearchPolicy): FileEntry? {
        if (!entry.isDirectory) return entry.takeIf { isPreviewableEntry(it) }
        return when (policy) {
            ThumbnailSearchPolicy.DIRECT_CHILD -> firstSupported(fileSource.listDirectory(entry.relativePath))
            ThumbnailSearchPolicy.RESOURCE_COVER -> findResourceCover(entry.relativePath)
        }
    }

    private suspend fun findResourceCover(root: String): FileEntry? {
        val queue = ArrayDeque<Pair<String, Int>>().apply { add(root to 0) }
        var visitedDirectories = 0
        while (queue.isNotEmpty() && visitedDirectories++ < MAX_RESOURCE_DIRECTORIES) {
            val (path, depth) = queue.removeFirst()
            val entries = fileSource.listDirectory(path).sortedBy { it.name.lowercase() }
            firstSupported(entries)?.let { return it }
            if (depth < MAX_RESOURCE_DEPTH) {
                entries.filter { it.isDirectory }.forEach { queue.addLast(it.relativePath to depth + 1) }
            }
        }
        return null
    }

    private fun firstSupported(entries: List<FileEntry>): FileEntry? =
        entries.firstOrNull { !it.isDirectory && MediaFormats.isImage(it.extension) }
            ?: entries.firstOrNull { !it.isDirectory && MediaFormats.isVideo(it.extension) }
            ?: entries.firstOrNull { !it.isDirectory && isPdf(it.extension) }
            ?: entries.firstOrNull { !it.isDirectory && MediaFormats.isArchive(it.archiveExtension()) }

    private fun isPreviewableEntry(entry: FileEntry): Boolean =
        MediaFormats.isPreviewable(entry.extension) || isPdf(entry.extension) ||
            MediaFormats.isArchive(entry.archiveExtension())

    private fun isPdf(extension: String): Boolean = extension.equals("pdf", ignoreCase = true)

    private suspend fun decodeImage(path: String, target: Int): Bitmap? {
        val bytes = fileSource.readFile(path)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun decodeVideo(entry: FileEntry, target: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(FileSourceMediaDataSource(fileSource, entry.relativePath, entry.size))
            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, target, target)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode video thumbnail for ${entry.relativePath}", e)
            null
        } finally {
            retriever.release()
        }
    }

    private suspend fun decodePdf(path: String, target: Int): Bitmap? {
        val appContext = context ?: return null
        val bytes = fileSource.readFile(path)
        if (bytes.isEmpty()) return null

        return PdfRenderer(appContext, bytes).use { renderer ->
            if (renderer.pageCount == 0) return@use null
            val pageSize = renderer.getPageSize(0)
            val scale = target.toFloat() / maxOf(pageSize.first, pageSize.second).coerceAtLeast(1)
            val width = (pageSize.first * scale).toInt().coerceAtLeast(1)
            val height = (pageSize.second * scale).toInt().coerceAtLeast(1)
            renderer.renderPage(0, width, height)
        }
    }

    private suspend fun decodeArchive(entry: FileEntry, target: Int): Bitmap? {
        if (!MediaFormats.isReadableArchive(entry.archiveExtension())) return null
        if (entry.size > MAX_IN_MEMORY_ARCHIVE_THUMBNAIL_BYTES) return null
        val archiveBytes = fileSource.readFile(entry.relativePath)
        val (_, imageBytes) = ArchiveImageReader.firstImageEntry(archiveBytes, entry.archiveExtension()) ?: return null
        return PageBitmapLoader.decodeImageBytes(imageBytes, target, target)
    }

    companion object {
        private const val TAG = "EntryThumbLoader"
        const val MAX_RESOURCE_DEPTH = 4
        const val MAX_RESOURCE_DIRECTORIES = 64
    }
}

internal class FileSourceMediaDataSource(
    private val source: FileSource,
    private val path: String,
    private val length: Long,
) : MediaDataSource() {
    override fun getSize(): Long = length
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= length) return -1
        val requested = minOf(size.toLong(), length - position).toInt()
        val bytes = runBlocking(Dispatchers.IO) { source.readRange(path, position, requested.toLong()) }
        if (bytes.isEmpty()) return -1
        bytes.copyInto(buffer, offset, 0, bytes.size)
        return bytes.size
    }
    override fun close() = Unit
}
