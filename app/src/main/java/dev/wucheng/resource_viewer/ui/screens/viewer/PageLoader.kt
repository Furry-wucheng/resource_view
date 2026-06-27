package dev.wucheng.resource_viewer.ui.screens.viewer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 合并同一页面的并发请求，并按内存占用维护 LRU 缓存。
 *
 * 网络图片的当前页和预取页可能在同一时间请求同一内容。这里保证底层只加载一次，
 * 同时避免已经浏览过的页面在重新进入组合时再次访问 SMB。
 */
class PageLoader<K : Any, V : Any>(
    private val maxCacheSize: Long,
    private val sizeOf: (V) -> Long,
    private val load: suspend (K) -> V,
) {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<K, V>(0, 0.75f, true)
    private val inFlight = mutableMapOf<K, CompletableDeferred<V>>()
    private var cacheSize = 0L

    init {
        require(maxCacheSize > 0) { "maxCacheSize must be positive" }
    }

    suspend fun get(key: K): V {
        val request = mutex.withLock {
            cache[key]?.let { return it }
            inFlight[key]?.let { return@withLock Request(it, isOwner = false) }

            val deferred = CompletableDeferred<V>()
            inFlight[key] = deferred
            Request(deferred, isOwner = true)
        }

        if (!request.isOwner) return request.deferred.await()

        return try {
            val value = load(key)
            mutex.withLock {
                put(key, value)
                inFlight.remove(key)
            }
            request.deferred.complete(value)
            value
        } catch (throwable: Throwable) {
            mutex.withLock { inFlight.remove(key) }
            request.deferred.completeExceptionally(throwable)
            throw throwable
        }
    }

    suspend fun isCached(key: K): Boolean = mutex.withLock { cache.containsKey(key) }

    suspend fun clear() {
        mutex.withLock {
            cache.clear()
            cacheSize = 0L
        }
    }

    private fun put(key: K, value: V) {
        val valueSize = normalizedSizeOf(value)
        if (valueSize > maxCacheSize) return

        cache.remove(key)?.let { cacheSize -= normalizedSizeOf(it) }
        cache[key] = value
        cacheSize += valueSize

        val iterator = cache.entries.iterator()
        while (cacheSize > maxCacheSize && cache.size > 1 && iterator.hasNext()) {
            val eldest = iterator.next()
            cacheSize -= normalizedSizeOf(eldest.value)
            iterator.remove()
        }
    }

    private fun normalizedSizeOf(value: V): Long = sizeOf(value).coerceAtLeast(0L)

    private data class Request<V>(
        val deferred: CompletableDeferred<V>,
        val isOwner: Boolean,
    )
}
