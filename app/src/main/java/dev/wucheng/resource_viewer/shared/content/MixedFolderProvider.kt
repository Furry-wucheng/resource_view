package dev.wucheng.resource_viewer.shared.content

import android.graphics.Bitmap
import androidx.media3.datasource.DataSource
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import java.io.File

/**
 * 混合文件夹内容提供者。
 * 同时支持图片和视频文件，实现图片/视频无缝浏览。
 *
 * 图片通过 ContentProvider.loadPage() 返回 Bitmap，
 * 视频通过 ViewerItem.Video + VideoMediaSource 由 ExoPlayer 渲染。
 *
 * @param recursive 是否递归扫描子目录，用于 GALLERY/CHAPTER_GALLERY 组织模式。
 */
class MixedFolderProvider(
    private val fileSource: FileSource,
    private val relativePath: String,
    private val sourceId: String,
    private val videoDataSourceFactory: DataSource.Factory? = null,
    private val recursive: Boolean = false,
    pageCacheDirectory: File? = null,
    pageCacheLimitBytes: Long = 500L * 1024 * 1024,
) : ContentProvider {
    companion object {
        private val IMAGE_EXTENSIONS = MediaFormats.imageExtensions
        private val VIDEO_EXTENSIONS = MediaFormats.videoExtensions
    }

    /** 混合文件列表（图片+视频，按名称排序） */
    private var mixedEntries: List<FileEntry>? = null

    /** 图片在混合列表中的索引映射到图片文件列表的索引 */
    private var imageFileList: List<String>? = null
    private val bitmapLoader = PageBitmapLoader(fileSource, pageCacheDirectory, pageCacheLimitBytes)

    override val pageCount: Int
        get() = getMixedEntries().size

    /**
     * 获取混合文件列表（惰性加载）。
     */
    private fun getMixedEntries(): List<FileEntry> {
        return mixedEntries ?: loadMixedEntries().also { mixedEntries = it }
    }

    private fun getImageFileList(): List<String> {
        return imageFileList ?: getMixedEntries()
            .filter { !it.isDirectory && it.extension.lowercase() in IMAGE_EXTENSIONS }
            .map { it.relativePath }
            .also { imageFileList = it }
    }

    private fun loadMixedEntries(): List<FileEntry> {
        return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            collectMixedEntries(relativePath)
        }
    }

    private suspend fun collectMixedEntries(path: String): List<FileEntry> {
        val entries = fileSource.listDirectory(path)
        val supported = entries.filter { !it.isDirectory && isSupported(it.extension) }
        if (!recursive) return supported.sortedBy { it.relativePath }

        val nested = entries
            .filter { it.isDirectory }
            .flatMap { collectMixedEntries(it.relativePath) }
        return (supported + nested).sortedBy { it.relativePath }
    }

    private fun isSupported(ext: String): Boolean {
        val lower = ext.lowercase()
        return lower in IMAGE_EXTENSIONS || lower in VIDEO_EXTENSIONS
    }

    /**
     * 构建 ViewerItem 列表（混合图片和视频）。
     * 用于 ViewerViewModel 设置查看器内容。
     */
    fun buildViewerItems(): List<ViewerItem> {
        val entries = getMixedEntries()
        val providerKey = "mixed:$sourceId:$relativePath"
        var imageIndex = 0

        return entries.map { entry ->
            val ext = entry.extension.lowercase()
            if (ext in VIDEO_EXTENSIONS) {
                ViewerItem.Video(
                    title = entry.name,
                    videoSource = createVideoSource(entry),
                )
            } else {
                val currentImageIndex = imageIndex
                imageIndex++
                ViewerItem.ImagePage(
                    title = entry.name,
                    pageIndex = currentImageIndex,
                    providerKey = providerKey,
                    extension = entry.extension,
                )
            }
        }
    }

    /**
     * 查找指定文件在混合列表中的索引。
     */
    fun findIndex(filePath: String): Int {
        return getMixedEntries().indexOfFirst { it.relativePath == filePath }.coerceAtLeast(0)
    }

    /**
     * 判断指定文件是否为视频。
     */
    fun isVideo(filePath: String): Boolean {
        val ext = filePath.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    private fun createVideoSource(entry: FileEntry): VideoMediaSource {
        val factory = videoDataSourceFactory
        return if (factory != null) {
            VideoMediaSource.SmbFile(
                dataSourceFactory = factory,
                relativePath = entry.relativePath,
                fileSize = entry.size,
            )
        } else {
            VideoMediaSource.LocalFile(entry.relativePath)
        }
    }

    /**
     * 加载指定图片页（跳过视频项）。
     * @param imageIndex 图片在图片文件列表中的索引（不是混合列表的索引）
     */
    override suspend fun loadPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val files = getImageFileList()
        require(index in 0 until files.size) { "Image index $index out of range [0, ${files.size})" }

        val filePath = files[index]
        val entry = getMixedEntries().first { it.relativePath == filePath }
        return bitmapLoader.load(entry, targetWidth, targetHeight)
    }

    /**
     * 获取指定图片页的文件扩展名。
     */
    fun getPageExtension(index: Int): String {
        val files = getImageFileList()
        require(index in 0 until files.size) { "Image index $index out of range [0, ${files.size})" }
        return files[index].substringAfterLast('.', "").lowercase()
    }

    /**
     * 获取指定图片页的文件 URI。
     * 用于 Coil 加载动画图片（GIF/animated WebP）。
     */
    suspend fun getPageUri(index: Int): android.net.Uri {
        val files = getImageFileList()
        require(index in 0 until files.size) { "Image index $index out of range [0, ${files.size})" }
        val filePath = files[index]
        val entry = getMixedEntries().first { it.relativePath == filePath }
        return android.net.Uri.fromFile(bitmapLoader.ensureLocalFile(entry))
    }

    override fun dispose() {
        // No-op
    }

}
