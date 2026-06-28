package dev.wucheng.resource_viewer.shared.thumbnail

import android.graphics.Bitmap
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import kotlinx.coroutines.CancellationException

/**
 * 全局缩略图加载管理器（单例）。
 * 由 Koin 注册为 single，所有 ViewModel 共享同一实例。
 *
 * 三层缓存：
 * 1. 内存 LRU（key: "sourceId:relativePath"，共享）
 * 2. 失败记录集（避免重复尝试已知缺失）
 * 3. FileBrowserThumbnailDiskCache（磁盘，Koin 单例）
 */
class ThumbnailLoadManager(
    private val diskCache: FileBrowserThumbnailDiskCache?,
    maxConcurrency: Int = 4,
    maxCacheSize: Int = 64,
) {
    private val pool = ThumbnailTaskPool(maxConcurrency.coerceIn(1, 8))
    private var thumbnailLoader: FileEntryThumbnailLoader? = null

    private val cache = object : LinkedHashMap<String, Bitmap>(maxCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > maxCacheSize
    }

    private val misses = mutableSetOf<String>()

    fun setFileSource(fileSource: FileSource) {
        thumbnailLoader = FileEntryThumbnailLoader(fileSource)
    }

    fun configureCapacity(cacheLimitMB: Int) {
        diskCache?.configureCapacity(cacheLimitMB)
    }

    private fun cacheKey(sourceId: String, relativePath: String): String =
        "$sourceId:$relativePath"

    suspend fun load(
        sourceId: String,
        entry: FileEntry,
        policy: ThumbnailSearchPolicy = ThumbnailSearchPolicy.DIRECT_CHILD,
    ): Bitmap? {
        val key = cacheKey(sourceId, entry.relativePath)
        synchronized(cache) { cache[key] }?.let { return it }
        if (synchronized(misses) { key in misses }) return null

        val loader = thumbnailLoader ?: return null

        return try {
            pool.run {
                val cached = diskCache?.get(sourceId, entry, policy)
                val bitmap = if (cached?.isCached == true) {
                    cached.bitmap
                } else {
                    loader.load(entry, policy = policy)?.also {
                        diskCache?.put(sourceId, entry, policy, it)
                    }
                }
                if (bitmap == null) {
                    synchronized(misses) { misses += key }
                } else {
                    synchronized(cache) { cache[key] = bitmap }
                }
                bitmap
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            synchronized(misses) { misses += key }
            null
        }
    }

    suspend fun load(
        sourceId: String,
        path: String,
        policy: ThumbnailSearchPolicy = ThumbnailSearchPolicy.DIRECT_CHILD,
    ): Bitmap? {
        val entry = FileEntry(
            path.substringAfterLast("/"),
            path,
            false,
            0,
            0L,
            path.substringAfterLast(".", ""),
        )
        return load(sourceId, entry, policy)
    }

    fun clear() {
        synchronized(cache) { cache.clear() }
        synchronized(misses) { misses.clear() }
    }
}
