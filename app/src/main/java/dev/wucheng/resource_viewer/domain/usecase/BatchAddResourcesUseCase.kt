package dev.wucheng.resource_viewer.domain.usecase

import android.content.Context
import android.util.Log
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.ThumbnailRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailTaskPool
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID

/**
 * 批量添加资源用例。
 *
 * 接收 ResourcePicker 勾选的路径列表 → 每条路径创建 Resource → 批量插入。
 * 插入后为每个资源并发生成缩略图，遵循设置中的 thumbnailConcurrency 配置。
 *
 * 注意：此实现遵循 doc/share/06-error-handling.md 中的 ScanResult 定义。
 */
class BatchAddResourcesUseCase(
    private val resourceRepository: ResourceRepository,
    private val detectOrganizationModeUseCase: DetectOrganizationModeUseCase,
    private val thumbnailRepository: ThumbnailRepository,
    private val context: Context,
    private val database: AppDatabase,
) {
    /**
     * 批量添加资源。
     *
     * @param fileSource 文件源
     * @param source 数据源配置
     * @param paths 要添加的相对路径列表
     * @param organizationMode 可选的组织模式覆盖（null 表示自动检测）
     * @param tagIds 可选的标签 ID 列表
     * @return ScanResult 包含成功/跳过/失败统计
     */
    suspend operator fun invoke(
        fileSource: FileSource,
        source: Source,
        paths: List<String>,
        organizationMode: dev.wucheng.resource_viewer.data.local.converter.OrganizationMode? = null,
        tagIds: List<String> = emptyList(),
    ): Result<ScanResult> {
        var successCount = 0
        var skipCount = 0
        val failures = mutableListOf<Pair<String, dev.wucheng.resource_viewer.domain.error.DomainError>>()
        val batch = mutableListOf<ResourceEntity>()

        for (path in paths) {
            try {
                val entry = fileSource.stat(path)
                if (entry == null) {
                    skipCount++
                    continue
                }

                val entity = createResourceEntity(fileSource, source, entry, organizationMode)
                if (entity != null) {
                    batch.add(entity)
                    successCount++
                } else {
                    skipCount++
                }
            } catch (e: Exception) {
                failures.add(
                    path to dev.wucheng.resource_viewer.domain.error.DomainError.DatabaseError(
                        "Failed to process path: $path",
                        e,
                    )
                )
            }
        }

        // Batch insert
        if (batch.isNotEmpty()) {
            when (val insertResult = resourceRepository.insertAll(batch)) {
                is Result.Ok -> {
                    // 关联标签
                    if (tagIds.isNotEmpty()) {
                        for (entity in batch) {
                            resourceRepository.setResourceTags(entity.id, tagIds)
                        }
                    }
                    // 生成缩略图
                    generateThumbnails(batch, fileSource)
                }
                is Result.Err -> {
                    batch.forEach { entity ->
                        failures.add(entity.name to insertResult.error)
                    }
                    successCount = 0
                }
            }
        }

        return Result.Ok(ScanResult(successCount, skipCount, failures))
    }

    /**
     * 为插入的资源批量并发生成缩略图。
     * 使用 ThumbnailTaskPool 控制并发数，遵循设置中的 thumbnailConcurrency 配置。
     * 异常会被记录但不会中断主流程。
     */
    private suspend fun generateThumbnails(
        entities: List<ResourceEntity>,
        fileSource: FileSource,
    ) {
        // 读取并发配置
        val config = database.appConfigDao().getConfig().first()
        val concurrency = config?.thumbnailConcurrency ?: 4
        val pool = ThumbnailTaskPool(concurrency)

        // 封面缓存目录（移出 image_cache/ 避免被误删）
        val cacheDir = context.cacheDir.resolve("thumbnails/resources")
        cacheDir.mkdirs()

        coroutineScope {
            entities.map { entity ->
                async {
                    pool.run {
                        try {
                            val resource = entity.toDomain()
                            when (val result = thumbnailRepository.generateThumbnail(resource, fileSource, cacheDir)) {
                                is Result.Ok -> {
                                    val thumbFile = result.value
                                    if (thumbFile != null) {
                                        resourceRepository.updateThumbnail(entity.id, thumbFile.absolutePath)
                                    }
                                }
                                is Result.Err -> {
                                    Log.e(
                                        TAG,
                                        "Thumbnail generation failed for ${entity.name}: ${result.error}",
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Thumbnail generation exception for ${entity.name}",
                                e,
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * 根据文件条目创建 ResourceEntity。
     */
    private suspend fun createResourceEntity(
        fileSource: FileSource,
        source: Source,
        entry: dev.wucheng.resource_viewer.domain.model.FileEntry,
        overrideOrgMode: dev.wucheng.resource_viewer.data.local.converter.OrganizationMode? = null,
    ): ResourceEntity? {
        val type = when {
            entry.isDirectory -> ResourceType.FOLDER
            entry.extension.lowercase() in setOf("pdf") -> ResourceType.PDF
            MediaFormats.isVideo(entry.extension) -> ResourceType.VIDEO
            entry.extension.lowercase() in setOf("zip", "rar", "7z", "cbz", "cbr") -> ResourceType.ARCHIVE
            else -> return null
        }

        val organizationMode = overrideOrgMode ?: if (type == ResourceType.FOLDER) {
            detectOrganizationModeUseCase(fileSource, entry.relativePath)
        } else {
            null
        }

        return ResourceEntity(
            id = UUID.randomUUID().toString(),
            sourceId = source.id,
            name = entry.name,
            type = type,
            organizationMode = organizationMode,
            relativePath = entry.relativePath,
            fileSize = entry.size,
            isAvailable = true,
            lastScannedAt = System.currentTimeMillis(),
        )
    }

    private fun ResourceEntity.toDomain(): dev.wucheng.resource_viewer.domain.model.Resource {
        return dev.wucheng.resource_viewer.domain.model.Resource(
            id = id,
            sourceId = sourceId,
            sourceName = "",
            name = name,
            type = type,
            organizationMode = organizationMode,
            relativePath = relativePath,
            fileSize = fileSize,
            fileCount = null,
            favorited = favorited,
            isAvailable = isAvailable,
            lastScannedAt = lastScannedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            thumbnailPath = thumbnailPath,
            tags = emptyList(),
        )
    }

    companion object {
        private const val TAG = "BatchAddResourcesUseCase"
    }
}
