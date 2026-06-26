package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.util.UUID

/**
 * 批量添加资源用例。
 *
 * 接收 ResourcePicker 勾选的路径列表 → 每条路径创建 Resource → 批量插入。
 *
 * 注意：此实现遵循 doc/share/06-error-handling.md 中的 ScanResult 定义。
 */
class BatchAddResourcesUseCase(
    private val resourceRepository: ResourceRepository,
    private val detectOrganizationModeUseCase: DetectOrganizationModeUseCase,
) {
    /**
     * 批量添加资源。
     *
     * @param fileSource 文件源
     * @param source 数据源配置
     * @param paths 要添加的相对路径列表
     * @return ScanResult 包含成功/跳过/失败统计
     */
    suspend operator fun invoke(
        fileSource: FileSource,
        source: Source,
        paths: List<String>,
    ): Result<ScanResult> {
        var successCount = 0
        var skipCount = 0
        val failures = mutableListOf<Pair<String, DomainError>>()
        val batch = mutableListOf<ResourceEntity>()

        for (path in paths) {
            try {
                val entry = fileSource.stat(path)
                if (entry == null) {
                    skipCount++
                    continue
                }

                val entity = createResourceEntity(fileSource, source, entry)
                if (entity != null) {
                    batch.add(entity)
                    successCount++
                } else {
                    skipCount++
                }
            } catch (e: Exception) {
                failures.add(path to DomainError.DatabaseError("Failed to process path: $path", e))
            }
        }

        // Batch insert
        if (batch.isNotEmpty()) {
            when (val insertResult = resourceRepository.insertAll(batch)) {
                is Result.Ok -> { /* success */ }
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
     * 根据文件条目创建 ResourceEntity。
     */
    private suspend fun createResourceEntity(
        fileSource: FileSource,
        source: Source,
        entry: dev.wucheng.resource_viewer.domain.model.FileEntry,
    ): ResourceEntity? {
        val type = when {
            entry.isDirectory -> ResourceType.FOLDER
            entry.extension.lowercase() in setOf("pdf") -> ResourceType.PDF
            entry.extension.lowercase() in setOf("mp4", "avi", "mkv", "mov", "wmv") -> ResourceType.VIDEO
            entry.extension.lowercase() in setOf("zip", "rar", "7z", "cbz", "cbr") -> ResourceType.ARCHIVE
            else -> return null
        }

        val organizationMode = if (type == ResourceType.FOLDER) {
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
}
