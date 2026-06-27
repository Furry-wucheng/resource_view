package dev.wucheng.resource_viewer.data.cache

import android.content.Context
import coil3.ImageLoader
import dev.wucheng.resource_viewer.shared.thumbnail.FileBrowserThumbnailDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 统一缓存管理器。
 * 提供各缓存分类的大小查询和清理功能。
 *
 * 缓存目录结构：
 * - thumbnails/resources/  : 封面缓存（默认永久存储，可设置容量限制）
 * - image_cache/pages/     : SMB 页面文件缓存
 * - image_cache/file_browser/ : 文件浏览器缩略图缓存
 */
class CacheManager(
    private val context: Context,
    private val coilImageLoader: ImageLoader,
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache,
) {
    /**
     * 各缓存分类的大小信息。
     */
    data class CacheSizes(
        val coverCacheBytes: Long,      // thumbnails/resources/
        val pageCacheBytes: Long,       // image_cache/pages/
        val thumbnailCacheBytes: Long,  // image_cache/file_browser/
    )

    /**
     * 获取各缓存分类的大小。
     */
    suspend fun getCacheSizes(): CacheSizes = withContext(Dispatchers.IO) {
        CacheSizes(
            coverCacheBytes = getDirectorySize(getCoverCacheDir()),
            pageCacheBytes = getDirectorySize(getPageCacheDir()),
            thumbnailCacheBytes = getDirectorySize(getThumbnailCacheDir()),
        )
    }

    /**
     * 清理封面缓存（thumbnails/resources/）。
     */
    suspend fun clearCoverCache() = withContext(Dispatchers.IO) {
        getCoverCacheDir().deleteRecursively()
    }

    /**
     * 清理 SMB 页面文件缓存（image_cache/pages/）。
     */
    suspend fun clearPageCache() = withContext(Dispatchers.IO) {
        getPageCacheDir().deleteRecursively()
    }

    /**
     * 清理文件浏览器缩略图缓存（image_cache/file_browser/）。
     */
    suspend fun clearThumbnailCache() = withContext(Dispatchers.IO) {
        getThumbnailCacheDir().deleteRecursively()
    }

    /**
     * 获取封面缓存目录。
     */
    private fun getCoverCacheDir(): File {
        return context.cacheDir.resolve("thumbnails/resources")
    }

    /**
     * 获取页面缓存目录。
     */
    private fun getPageCacheDir(): File {
        return context.cacheDir.resolve("image_cache/pages")
    }

    /**
     * 获取缩略图缓存目录。
     */
    private fun getThumbnailCacheDir(): File {
        return context.cacheDir.resolve("image_cache/file_browser")
    }

    /**
     * 计算目录大小。
     */
    private fun getDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
}
