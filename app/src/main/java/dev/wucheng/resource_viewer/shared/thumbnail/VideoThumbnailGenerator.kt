package dev.wucheng.resource_viewer.shared.thumbnail

import android.media.MediaMetadataRetriever
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File
import java.io.FileOutputStream

/**
 * 视频缩略图生成器。
 * 使用 [MediaMetadataRetriever] 提取视频首帧作为缩略图。
 *
 * 支持本地和远程（SMB）视频文件：
 * - 通过 [FileSourceMediaDataSource] 分块读取视频数据，避免全量读取大文件
 * - 使用 [MediaMetadataRetriever.setDataSource] 配合 [MediaDataSource] 提取首帧
 *
 * 支持复用 FileBrowserThumbnailDiskCache 中已有的缓存，避免重复 I/O。
 *
 * 注意：此实现遵循 doc/share/02-interfaces.md §4 VideoThumbnailGenerator 契约。
 */
class VideoThumbnailGenerator(
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
) : ThumbnailGenerator {

    override fun canHandle(type: ResourceType): Boolean {
        return type == ResourceType.VIDEO
    }

    /**
     * 生成缩略图，返回缓存文件路径。
     * 优先复用已有缓存：
     * 1. 检查封面缓存目录是否已有该资源的缩略图
     * 2. 检查 FileBrowserThumbnailDiskCache 是否有缓存
     * 3. 使用 FileSourceMediaDataSource 分块读取提取首帧
     */
    override suspend fun generate(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): File? {
        return try {
            val outputFile = File(cacheDir, "thumb_${resource.id}.jpg")

            // 1. 检查封面缓存是否已存在
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile
            }

            val entry = FileEntry(
                resource.name,
                resource.relativePath,
                false,
                resource.fileSize ?: 0,
                resource.updatedAt,
            )

            // 2. 检查 FileBrowserThumbnailDiskCache 是否有缓存
            if (thumbnailDiskCache != null) {
                val cached = thumbnailDiskCache.get(
                    resource.sourceId,
                    entry,
                    ThumbnailSearchPolicy.RESOURCE_COVER,
                )
                if (cached.isCached && cached.bitmap != null) {
                    FileOutputStream(outputFile).use { fos ->
                        cached.bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    cached.bitmap.recycle()
                    return outputFile
                }
            }

            // 3. 使用 FileSourceMediaDataSource 分块读取，避免全量加载视频
            val bitmap = extractFirstFrame(
                fileSource,
                resource.relativePath,
                resource.fileSize ?: 0,
            ) ?: return null

            // 保存到缓存目录
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
            }

            // 4. 写入 FileBrowserThumbnailDiskCache
            if (thumbnailDiskCache != null) {
                thumbnailDiskCache.put(
                    resource.sourceId,
                    entry,
                    ThumbnailSearchPolicy.RESOURCE_COVER,
                    bitmap,
                )
            }

            bitmap.recycle()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 使用 [FileSourceMediaDataSource] 分块读取视频数据，提取首帧。
     * 相比全量 [FileSource.readFile]，此方法仅按需读取视频头部数据，
     * 避免对大视频文件（尤其是 SMB）造成整文件下载。
     */
    private fun extractFirstFrame(
        fileSource: FileSource,
        path: String,
        size: Long,
    ): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(FileSourceMediaDataSource(fileSource, path, size))
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
