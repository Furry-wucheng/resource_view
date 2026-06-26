package dev.wucheng.resource_viewer.shared.thumbnail

import android.media.MediaMetadataRetriever
import android.os.Build
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
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
 * 注意：此实现遵循 doc/share/02-interfaces.md §4 VideoThumbnailGenerator 契约。
 */
class VideoThumbnailGenerator : ThumbnailGenerator {

    override fun canHandle(type: ResourceType): Boolean {
        return type == ResourceType.VIDEO
    }

    override suspend fun generate(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): File? {
        return try {
            // 读取视频文件内容
            val videoBytes = fileSource.readFile(resource.relativePath)

            // 使用 MediaMetadataRetriever 提取首帧
            val bitmap = extractFirstFrame(videoBytes) ?: return null

            // 保存到缓存目录
            val outputFile = File(cacheDir, "thumb_${resource.id}.jpg")
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
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
