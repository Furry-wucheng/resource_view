package dev.wucheng.resource_viewer.shared.thumbnail

import android.media.MediaMetadataRetriever
import android.os.Build
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File
import java.io.FileOutputStream

/**
 * 视频缩略图生成器。
 * 使用 [MediaMetadataRetriever] 提取视频首帧作为缩略图。
 *
 * 支持本地和远程（SMB）视频文件：
 * - 通过 [FileSource.readFile] 获取视频字节数据
 * - 使用 [MediaMetadataRetriever.setDataSource] 提取首帧
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
     * 3. 生成新的缩略图并写入两份缓存
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

            val entry = FileEntry(resource.name, resource.relativePath, false, resource.fileSize ?: 0, resource.updatedAt)
            val loader = FileEntryThumbnailLoader(fileSource)

            // 2. 检查 FileBrowserThumbnailDiskCache 是否有缓存
            if (thumbnailDiskCache != null) {
                val cached = thumbnailDiskCache.get(resource.sourceId, entry, ThumbnailSearchPolicy.RESOURCE_COVER)
                if (cached.isCached && cached.bitmap != null) {
                    FileOutputStream(outputFile).use { fos ->
                        cached.bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    cached.bitmap.recycle()
                    return outputFile
                }
            }

            // 3. 读取视频文件内容
            val videoBytes = fileSource.readFile(resource.relativePath)

            // 4. 使用 MediaMetadataRetriever 提取首帧
            val bitmap = extractFirstFrame(videoBytes) ?: return null

            // 保存到缓存目录
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
            }

            // 5. 写入 FileBrowserThumbnailDiskCache
            if (thumbnailDiskCache != null) {
                thumbnailDiskCache.put(resource.sourceId, entry, ThumbnailSearchPolicy.RESOURCE_COVER, bitmap)
            }

            bitmap.recycle()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从视频字节数据中提取首帧。
     * 先写入临时文件，再使用 MediaMetadataRetriever 提取。
     *
     * @param videoBytes 视频文件字节数组
     * @return 首帧 Bitmap，失败返回 null
     */
    private fun extractFirstFrame(videoBytes: ByteArray): android.graphics.Bitmap? {
        // 写入临时文件
        val tempFile = File.createTempFile("video_thumb_", ".tmp")
        return try {
            tempFile.writeBytes(videoBytes)
            extractFirstFrameFromFile(tempFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            tempFile.delete()
        }
    }

    /**
     * 从文件路径提取首帧。
     * @param filePath 视频文件路径
     * @return 首帧 Bitmap，失败返回 null
     */
    private fun extractFirstFrameFromFile(filePath: String): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
