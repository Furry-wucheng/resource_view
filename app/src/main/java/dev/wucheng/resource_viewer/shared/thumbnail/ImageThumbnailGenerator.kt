package dev.wucheng.resource_viewer.shared.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File
import java.io.FileOutputStream

/**
 * 图片缩略图生成器。
 * 读取图片文件 → resize → JPEG 压缩到磁盘缓存。
 *
 * 注意：此实现遵循 doc/mvp/M23-home-grid.md 中的 M23.5 子任务。
 */
class ImageThumbnailGenerator(
    private val context: Context,
) : ThumbnailGenerator {
    /** 缩略图最大尺寸（px） */
    private val maxThumbnailSize = 300

    /**
     * 是否可处理该资源类型。
     * @param type 资源类型
     * @return true 如果是文件夹类型（可能包含图片）
     */
    override fun canHandle(type: ResourceType): Boolean = type == ResourceType.FOLDER

    /**
     * 生成缩略图，返回缓存文件路径。
     * @param resource 资源
     * @param fileSource 文件源
     * @param cacheDir 缓存目录
     * @return 缩略图文件路径，失败返回 null
     */
    override suspend fun generate(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): File? {
        return try {
            // 列出文件夹中的图片文件
            val entries = fileSource.listDirectory(resource.relativePath)
            val imageEntry = entries.firstOrNull { entry ->
                val name = entry.name.lowercase()
                !entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))
            } ?: return null

            // 读取图片文件
            val imageBytes = fileSource.readFile(imageEntry.relativePath)
            if (imageBytes.isEmpty()) return null

            // 解码并缩放
            val bitmap = decodeAndScale(imageBytes) ?: return null

            // 保存到缓存
            val outputFile = getOutputFile(cacheDir, resource.id)
            saveBitmap(bitmap, outputFile)
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解码并缩放图片。
     */
    private fun decodeAndScale(imageBytes: ByteArray): Bitmap? {
        // 先获取尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        // 计算采样率
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1
        while (width / inSampleSize > maxThumbnailSize * 2 || height / inSampleSize > maxThumbnailSize * 2) {
            inSampleSize *= 2
        }

        // 解码
        options.apply {
            inJustDecodeBounds = false
            inSampleSize = inSampleSize
        }
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    }

    /**
     * 获取输出文件路径。
     */
    private fun getOutputFile(cacheDir: File, resourceId: String): File {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "thumb_${resourceId}.jpg")
    }

    /**
     * 保存 Bitmap 到文件。
     */
    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
    }
}
