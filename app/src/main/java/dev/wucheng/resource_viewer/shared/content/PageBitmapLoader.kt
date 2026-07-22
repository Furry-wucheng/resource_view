package dev.wucheng.resource_viewer.shared.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.filesource.LocalFileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class PageBitmapLoader(
    private val fileSource: FileSource,
    private val cacheDirectory: File? = null,
    private val cacheLimitBytes: Long = DEFAULT_CACHE_LIMIT,
) {
    suspend fun load(entry: FileEntry, targetWidth: Int, targetHeight: Int): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = when {
            fileSource is LocalFileSource -> {
                decodeFile(fileSource.resolveAbsoluteFile(entry.relativePath), targetWidth, targetHeight)
            }
            cacheDirectory != null -> {
                decodeFile(cacheFileInternal(entry), targetWidth, targetHeight)
            }
            else -> {
                decodeStreams(entry.relativePath, targetWidth, targetHeight)
            }
        }
        bitmap ?: throw IOException("Failed to decode image: ${entry.relativePath}")
    }

    suspend fun ensureLocalFile(entry: FileEntry): File = withContext(Dispatchers.IO) {
        when (fileSource) {
            is LocalFileSource -> fileSource.resolveAbsoluteFile(entry.relativePath)
            else -> {
                requireNotNull(cacheDirectory) { "Cache directory required for non-local file sources" }
                cacheFileInternal(entry)
            }
        }
    }

    private suspend fun cacheFileInternal(entry: FileEntry): File = cacheMutex.withLock {
        val directory = cacheDirectory!!.resolve("image_cache").apply { mkdirs() }
        val target = directory.resolve("${cacheKey(entry)}.${entry.extension.ifBlank { "img" }}")
        if (target.isFile && (entry.size <= 0 || target.length() == entry.size)) {
            target.setLastModified(System.currentTimeMillis())
            return@withLock target
        }
        val part = directory.resolve("${target.name}.part")
        var lastError: Exception? = null
        repeat(2) { attempt ->
            part.delete()
            try {
                fileSource.openInputStream(entry.relativePath).use { input ->
                    part.outputStream().buffered().use { output ->
                        val buffer = ByteArray(COPY_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                    }
                }
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true)
                    part.delete()
                }
                target.setLastModified(System.currentTimeMillis())
                evict(directory, target)
                return@withLock target
            } catch (cancellation: CancellationException) {
                part.delete()
                throw cancellation
            } catch (error: Exception) {
                part.delete()
                lastError = error
                if (attempt == 1) throw error
            }
        }
        throw lastError ?: IOException("File download failed")
    }

    private fun decodeFile(file: File, width: Int, height: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, width, height)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun decodeStreams(path: String, width: Int, height: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        fileSource.openInputStream(path).use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, width, height)
        options.inJustDecodeBounds = false
        return fileSource.openInputStream(path).use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun sampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1
        while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) sample *= 2
        return sample
    }

    private fun cacheKey(entry: FileEntry): String {
        val value = "page_v1|${fileSource.sourceId}|${entry.relativePath}|${entry.modifiedAt}|${entry.size}"
        return MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun evict(directory: File, keep: File) {
        val files = directory.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") } ?: return
        var size = files.sumOf(File::length)
        files.filter { it != keep }.sortedBy(File::lastModified).forEach { file ->
            if (size <= cacheLimitBytes) return
            size -= file.length()
            file.delete()
        }
    }

    companion object {
        fun decodeImageBytes(imageBytes: ByteArray, targetWidth: Int, targetHeight: Int): Bitmap? {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            options.inSampleSize = sampleSizeStatic(options.outWidth, options.outHeight, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        }

        private fun sampleSizeStatic(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Int {
            var sample = 1
            while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) sample *= 2
            return sample
        }

        private val cacheMutex = Mutex()
        private const val COPY_BUFFER_SIZE = 256 * 1024
        private const val DEFAULT_CACHE_LIMIT = 500L * 1024 * 1024
    }
}
