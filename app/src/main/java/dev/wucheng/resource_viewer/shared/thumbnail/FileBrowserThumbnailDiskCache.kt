package dev.wucheng.resource_viewer.shared.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.wucheng.resource_viewer.domain.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class ThumbnailDiskResult(val isCached: Boolean, val bitmap: Bitmap?)

class FileBrowserThumbnailDiskCache(context: Context) {
    private val directory = context.cacheDir.resolve("image_cache/file_browser")
    private val mutex = Mutex()
    @Volatile private var capacityBytes = DEFAULT_CAPACITY_BYTES

    fun configureCapacity(cacheLimitMb: Int) {
        capacityBytes = cacheLimitMb.coerceAtLeast(500).toLong() * 1024 * 1024
    }

    suspend fun get(
        sourceId: String,
        entry: FileEntry,
        policy: ThumbnailSearchPolicy,
    ): ThumbnailDiskResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = key(sourceId, entry, policy)
            val image = directory.resolve("$key.jpg")
            val empty = directory.resolve("$key.none")
            when {
                image.isFile -> {
                    val bitmap = BitmapFactory.decodeFile(image.absolutePath)
                    if (bitmap == null) {
                        image.delete()
                        ThumbnailDiskResult(false, null)
                    } else {
                        image.setLastModified(System.currentTimeMillis())
                        ThumbnailDiskResult(true, bitmap)
                    }
                }
                empty.isFile -> {
                    empty.setLastModified(System.currentTimeMillis())
                    ThumbnailDiskResult(true, null)
                }
                else -> ThumbnailDiskResult(false, null)
            }
        }
    }

    suspend fun put(
        sourceId: String,
        entry: FileEntry,
        policy: ThumbnailSearchPolicy,
        bitmap: Bitmap?,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            directory.mkdirs()
            val key = key(sourceId, entry, policy)
            val target = directory.resolve(if (bitmap == null) "$key.none" else "$key.jpg")
            val opposite = directory.resolve(if (bitmap == null) "$key.jpg" else "$key.none")
            opposite.delete()
            if (bitmap == null) {
                target.writeBytes(ByteArray(0))
            } else {
                val temporary = directory.resolve("$key.tmp")
                temporary.outputStream().buffered().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 86, it) }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
            }
            target.setLastModified(System.currentTimeMillis())
            evictIfNeeded()
        }
    }

    private fun evictIfNeeded() {
        val files = directory.listFiles()?.filter { it.isFile } ?: return
        var size = files.sumOf(File::length)
        if (size <= capacityBytes) return
        files.sortedBy(File::lastModified).forEach { file ->
            if (size <= capacityBytes) return
            size -= file.length()
            file.delete()
        }
    }

    private fun key(sourceId: String, entry: FileEntry, policy: ThumbnailSearchPolicy): String {
        val fingerprint = listOf(
            CACHE_VERSION,
            sourceId,
            entry.relativePath,
            entry.modifiedAt,
            entry.size,
            policy.name,
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(fingerprint.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val CACHE_VERSION = "browser_preview_v1"
        private const val DEFAULT_CAPACITY_BYTES = 500L * 1024 * 1024
    }
}
