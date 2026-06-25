package dev.wucheng.resource_viewer.data.repository

import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.asErr
import dev.wucheng.resource_viewer.domain.error.asOk
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailGenerator
import java.io.File

/**
 * 缩略图仓库。
 * 综合多个 ThumbnailGenerator 策略，根据资源类型选择合适的生成器。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 RepositoryModule 契约。
 */
class ThumbnailRepository(
    private val thumbnailGenerators: Set<ThumbnailGenerator>,
) {
    /**
     * 生成资源缩略图。
     * @param resource 资源
     * @param fileSource 文件源
     * @param cacheDir 缓存目录
     * @return 缩略图文件路径，如果无法生成则返回 null
     */
    suspend fun generateThumbnail(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): Result<File?> {
        return try {
            val generator = thumbnailGenerators.find { it.canHandle(resource.type) }
            if (generator != null) {
                val file = generator.generate(resource, fileSource, cacheDir)
                file.asOk()
            } else {
                null.asOk()
            }
        } catch (e: Exception) {
            DomainError.MediaLoadError(
                mediaType = when (resource.type) {
                    dev.wucheng.resource_viewer.data.local.converter.ResourceType.FOLDER -> dev.wucheng.resource_viewer.domain.error.MediaType.IMAGE
                    dev.wucheng.resource_viewer.data.local.converter.ResourceType.PDF -> dev.wucheng.resource_viewer.domain.error.MediaType.PDF
                    dev.wucheng.resource_viewer.data.local.converter.ResourceType.ARCHIVE -> dev.wucheng.resource_viewer.domain.error.MediaType.ARCHIVE
                    dev.wucheng.resource_viewer.data.local.converter.ResourceType.VIDEO -> dev.wucheng.resource_viewer.domain.error.MediaType.VIDEO
                },
                message = "Failed to generate thumbnail",
                cause = e,
            ).asErr()
        }
    }

    /**
     * 检查是否有可用的缩略图生成器。
     * @param resourceType 资源类型
     */
    fun hasGenerator(resourceType: dev.wucheng.resource_viewer.data.local.converter.ResourceType): Boolean {
        return thumbnailGenerators.any { it.canHandle(resourceType) }
    }
}
