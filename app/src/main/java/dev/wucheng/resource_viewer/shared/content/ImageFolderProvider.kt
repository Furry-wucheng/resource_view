package dev.wucheng.resource_viewer.shared.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.wucheng.resource_viewer.shared.filesource.FileSource

/**
 * 图片文件夹内容提供者。
 * 读取文件夹内所有图片文件并按名称排序，实现 ContentProvider 接口。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md 中的 M14.1 子任务。
 */
class ImageFolderProvider(
    private val fileSource: FileSource,
    private val relativePath: String,
    private val recursive: Boolean = false,
) : ContentProvider {
    /** 支持的图片扩展名 */
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

    /** 图片文件列表（按名称排序） - 惰性加载 */
    private var imageFiles: List<String>? = null

    /** 总页数 */
    override val pageCount: Int
        get() = getImageFiles().size

    /**
     * 获取图片文件列表（惰性加载）。
     */
    private fun getImageFiles(): List<String> {
        return imageFiles ?: loadFileList().also { imageFiles = it }
    }

    /**
     * 加载文件列表并按名称排序。
     */
    private fun loadFileList(): List<String> {
        return try {
            val entries = kotlinx.coroutines.runBlocking {
                collectImageEntries(relativePath)
            }
            entries
                .sortedBy { it.relativePath }
                .map { it.relativePath }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun collectImageEntries(path: String): List<dev.wucheng.resource_viewer.domain.model.FileEntry> {
        val entries = fileSource.listDirectory(path)
        val images = entries.filter { !it.isDirectory && it.extension.lowercase() in imageExtensions }
        if (!recursive) return images

        val nestedImages = entries
            .filter { it.isDirectory }
            .flatMap { collectImageEntries(it.relativePath) }
        return images + nestedImages
    }

    /**
     * 加载指定页并按目标尺寸渲染。
     * @param index 页码（0-based）
     * @param targetWidth 目标宽度（px）
     * @param targetHeight 目标高度（px）
     * @return 按目标尺寸解码的 Bitmap
     */
    override suspend fun loadPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val files = getImageFiles()
        require(index in 0 until files.size) { "Page index $index out of range [0, ${files.size})" }

        val filePath = files[index]
        val inputStream = fileSource.openInputStream(filePath)

        // 先将流读入 ByteArray，确保支持 mark/reset（SMB 流不支持 mark/reset）
        val imageBytes = inputStream.use { it.readBytes() }

        return decodeBitmap(imageBytes, targetWidth, targetHeight)
            ?: throw IllegalStateException("Failed to decode bitmap from $filePath")
    }

    /**
     * 释放资源。
     * ImageFolderProvider 不持有需要释放的资源。
     */
    override fun dispose() {
        // No-op for ImageFolderProvider
    }

    /**
     * 解码 Bitmap 并按目标尺寸缩放。
     * 使用 ByteArrayInputStream 支持 mark/reset，兼容 SMB 等不支持 mark/reset 的流。
     */
    private fun decodeBitmap(imageBytes: ByteArray, targetWidth: Int, targetHeight: Int): Bitmap? {
        // 先获取原始尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inJustDecodeBounds = false

        // 重新解码
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    }

    /**
     * 计算采样率以适应目标尺寸。
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
