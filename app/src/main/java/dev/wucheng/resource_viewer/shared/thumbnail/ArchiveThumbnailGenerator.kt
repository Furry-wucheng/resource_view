package dev.wucheng.resource_viewer.shared.thumbnail

import android.util.Log
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.content.ArchiveImageReader
import dev.wucheng.resource_viewer.shared.content.PageBitmapLoader
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File
import java.io.FileOutputStream

class ArchiveThumbnailGenerator(
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
) : ThumbnailGenerator {
    override fun canHandle(type: ResourceType): Boolean = type == ResourceType.ARCHIVE

    override suspend fun generate(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): File? {
        return try {
            val outputFile = getOutputFile(cacheDir, resource.id)
            if (outputFile.exists() && outputFile.length() > 0) return outputFile

            val entry = FileEntry(resource.name, resource.relativePath, false, resource.fileSize ?: 0L, resource.updatedAt)
            if (thumbnailDiskCache != null) {
                val cached = thumbnailDiskCache.get(resource.sourceId, entry, ThumbnailSearchPolicy.RESOURCE_COVER)
                if (cached.isCached && cached.bitmap != null) {
                    saveBitmap(cached.bitmap, outputFile)
                    return outputFile
                }
            }

            val archiveBytes = fileSource.readFile(resource.relativePath)
            val extension = resource.relativePath.substringAfterLast('.', "")
            val (_, imageBytes) = ArchiveImageReader.firstImageEntry(archiveBytes, extension) ?: return null
            val bitmap = PageBitmapLoader.decodeImageBytes(imageBytes, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                ?: return null

            thumbnailDiskCache?.put(resource.sourceId, entry, ThumbnailSearchPolicy.RESOURCE_COVER, bitmap)
            saveBitmap(bitmap, outputFile)
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Archive thumbnail generation failed for ${resource.name}", e)
            null
        }
    }

    private fun getOutputFile(cacheDir: File, resourceId: String): File {
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return File(cacheDir, "thumb_$resourceId.jpg")
    }

    private fun saveBitmap(bitmap: android.graphics.Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
    }

    private companion object {
        const val THUMBNAIL_SIZE = 320
        const val TAG = "ArchiveThumbGenerator"
    }
}
