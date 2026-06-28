package dev.wucheng.resource_viewer.shared.thumbnail

import android.graphics.Bitmap
import dev.wucheng.resource_viewer.domain.model.FileEntry

class ThumbnailLoadManager(
    private val sourceId: String,
    private val thumbnailLoader: FileEntryThumbnailLoader?,
    private val diskCache: FileBrowserThumbnailDiskCache?,
    maxConcurrency: Int = 4,
    maxCacheSize: Int = 32,
) {
    private val pool = ThumbnailTaskPool(maxConcurrency.coerceIn(1, 8))

    private val cache = object : LinkedHashMap<String, Bitmap>(maxCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > maxCacheSize
    }

    private val misses = mutableSetOf<String>()

    suspend fun load(entry: FileEntry): Bitmap? {
        synchronized(cache) { cache[entry.relativePath] }?.let { return it }
        if (synchronized(misses) { entry.relativePath in misses }) return null

        val loader = thumbnailLoader ?: return null

        return try {
            pool.run {
                val cached = diskCache?.get(sourceId, entry, ThumbnailSearchPolicy.DIRECT_CHILD)
                val bitmap = if (cached?.isCached == true) {
                    cached.bitmap
                } else {
                    loader.load(entry, policy = ThumbnailSearchPolicy.DIRECT_CHILD)?.also {
                        diskCache?.put(sourceId, entry, ThumbnailSearchPolicy.DIRECT_CHILD, it)
                    }
                }
                if (bitmap == null) {
                    synchronized(misses) { misses += entry.relativePath }
                } else {
                    synchronized(cache) { cache[entry.relativePath] = bitmap }
                }
                bitmap
            }
        } catch (_: Exception) {
            synchronized(misses) { misses += entry.relativePath }
            null
        }
    }

    suspend fun load(path: String): Bitmap? {
        val entry = FileEntry(
            path.substringAfterLast("/"),
            path,
            false,
            0,
            0L,
            path.substringAfterLast(".", ""),
        )
        return load(entry)
    }

    fun clear() {
        synchronized(cache) { cache.clear() }
        synchronized(misses) { misses.clear() }
    }
}
