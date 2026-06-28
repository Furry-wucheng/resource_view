package dev.wucheng.resource_viewer.shared.thumbnail

import android.content.Context
import android.graphics.Bitmap
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File
import java.io.FileOutputStream

/**
 * 图片缩略图生成器。
 * 读取图片文件 → resize → JPEG 压缩到磁盘缓存。
 *
 * 支持复用 FileBrowserThumbnailDiskCache 中已有的缓存，避免重复 I/O。
 *
 * 注意：此实现遵循 doc/mvp/M23-home-grid.md 中的 M23.5 子任务。
 */
class ImageThumbnailGenerator(
    private val context: Context,
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
) : ThumbnailGenerator {

    /**
     * 是否可处理该资源类型。
     * @param type 资源类型
     * @return true 如果是文件夹类型（可能包含图片）
     */
    override fun canHandle(type: ResourceType): Boolean = type == ResourceType.FOLDER

    /**
     * 生成缩略图，返回缓存文件路径。
     * 优先复用已有缓存：
     * 1. 检查封面缓存目录是否已有该资源的缩略图
     * 2. 检查 FileBrowserThumbnailDiskCache 是否有缓存
     * 3. 生成新的缩略图并写入两份缓存
     *
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
            val outputFile = getOutputFile(cacheDir, resource.id)

            // 1. 检查封面缓存是否已存在
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile
            }

            val entry = FileEntry(resource.name, resource.relativePath, true, 0, resource.updatedAt)
            val loader = FileEntryThumbnailLoader(fileSource)

            // 2. 找到实际的文件条目
            val previewEntry = loader.findPreviewEntry(entry, ThumbnailSearchPolicy.RESOURCE_COVER)

            // 3. 检查 FileBrowserThumbnailDiskCache 是否有缓存
            if (previewEntry != null && thumbnailDiskCache != null) {
                val cached = thumbnailDiskCache.get(resource.sourceId, previewEntry, ThumbnailSearchPolicy.RESOURCE_COVER)
                if (cached.isCached && cached.bitmap != null) {
                    saveBitmap(cached.bitmap, outputFile)
                    return outputFile
                }
            }

            // 4. 生成新的缩略图
            val bitmap = loader.load(entry, MAX_THUMBNAIL_SIZE, ThumbnailSearchPolicy.RESOURCE_COVER) ?: return null
            saveBitmap(bitmap, outputFile)

            // 5. 写入 FileBrowserThumbnailDiskCache
            if (previewEntry != null && thumbnailDiskCache != null) {
                thumbnailDiskCache.put(resource.sourceId, previewEntry, ThumbnailSearchPolicy.RESOURCE_COVER, bitmap)
            }

            outputFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getOutputFile(cacheDir: File, resourceId: String): File {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "thumb_${resourceId}.jpg")
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
    }

    private companion object {
        const val MAX_THUMBNAIL_SIZE = 320
    }
}
