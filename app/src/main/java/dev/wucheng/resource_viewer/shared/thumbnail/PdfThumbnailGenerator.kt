package dev.wucheng.resource_viewer.shared.thumbnail

import android.content.Context
import android.graphics.Bitmap
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.remote.pdf.PdfRenderer
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File
import java.io.FileOutputStream

/**
 * PDF 缩略图生成器。
 * 渲染 PDF 第 0 页 → resize → 保存为缩略图。
 *
 * 注意：此实现遵循 doc/mvp/M22-pdf-viewer.md 中的 M22.3 子任务。
 */
class PdfThumbnailGenerator(
    private val context: Context,
) : ThumbnailGenerator {
    /** 缩略图最大尺寸（px） */
    private val maxThumbnailSize = 300

    /**
     * 是否可处理该资源类型。
     * @param type 资源类型
     * @return true 如果是 PDF 类型
     */
    override fun canHandle(type: ResourceType): Boolean = type == ResourceType.PDF

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
            // 读取 PDF 文件
            val pdfBytes = fileSource.readFile(resource.relativePath)
            if (pdfBytes.isEmpty()) return null

            // 创建 PdfRenderer
            val renderer = PdfRenderer(context, pdfBytes)
            renderer.use { r ->
                if (r.pageCount == 0) return null

                // 渲染第 0 页
                val pageSize = r.getPageSize(0)
                val scale = maxThumbnailSize.toFloat() / maxOf(pageSize.first, pageSize.second)
                val width = (pageSize.first * scale).toInt()
                val height = (pageSize.second * scale).toInt()

                val bitmap = r.renderPage(0, width, height)

                // 保存到缓存
                val outputFile = getOutputFile(cacheDir, resource.id)
                saveBitmap(bitmap, outputFile)
                outputFile
            }
        } catch (e: Exception) {
            null
        }
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
