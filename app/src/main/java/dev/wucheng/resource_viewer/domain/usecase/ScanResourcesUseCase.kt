package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Progress
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

/**
 * 扫描资源用例。
 *
 * 遍历文件系统目录 → 生成 ResourceEntity → 批量插入 Room
 * → 通过 Progress Flow 发射进度更新 → 返回 ScanResult。
 *
 * 注意：此实现遵循 doc/share/06-error-handling.md 中的 Progress/ScanResult 定义。
 */
class ScanResourcesUseCase(
    private val resourceRepository: ResourceRepository,
    private val detectOrganizationModeUseCase: DetectOrganizationModeUseCase,
) {
    /**
     * 扫描指定目录，将发现的资源批量添加到数据库。
     *
     * @param fileSource 文件源
     * @param source 数据源配置
     * @param relativePath 要扫描的相对路径
     * @return Flow 发射进度更新，最终发射 ScanResult
     */
    operator fun invoke(
        fileSource: FileSource,
        source: Source,
        relativePath: String,
    ): Flow<Progress<ScanResult>> = flow {
        try {
            val entries = fileSource.listDirectory(relativePath)
            val total = entries.size
            var current = 0
            var successCount = 0
            var skipCount = 0
            val failures = mutableListOf<Pair<String, DomainError>>()

            // Process entries in batches
            val batch = mutableListOf<ResourceEntity>()

            for (entry in entries) {
                current++
                emit(Progress.Update(current, total))

                try {
                    val entity = createResourceEntity(fileSource, source, entry, relativePath)
                    if (entity != null) {
                        batch.add(entity)
                        successCount++
                    } else {
                        skipCount++
                    }
                } catch (e: Exception) {
                    failures.add(entry.name to DomainError.DatabaseError("Failed to process entry: ${entry.name}", e))
                }
            }

            // Batch insert
            if (batch.isNotEmpty()) {
                when (val result = resourceRepository.insertAll(batch)) {
                    is dev.wucheng.resource_viewer.domain.error.Result.Ok -> { /* success */ }
                    is dev.wucheng.resource_viewer.domain.error.Result.Err -> {
                        // If batch insert fails, record all as failures
                        batch.forEach { entity ->
                            failures.add(entity.name to result.error)
                        }
                        successCount = 0
                    }
                }
            }

            emit(Progress.Done(ScanResult(successCount, skipCount, failures)))
        } catch (e: Exception) {
            emit(Progress.Error(DomainError.FileNotFoundError("Failed to scan directory: $relativePath", e)))
        }
    }

    /**
     * 根据文件条目创建 ResourceEntity。
     * 文件夹会检测组织模式，非文件夹直接返回 null（跳过）。
     */
    private suspend fun createResourceEntity(
        fileSource: FileSource,
        source: Source,
        entry: FileEntry,
        parentPath: String,
    ): ResourceEntity? {
        val fullPath = if (parentPath.isEmpty()) entry.name else "$parentPath/${entry.name}"

        val type = when {
            entry.isDirectory -> ResourceType.FOLDER
            entry.extension.lowercase() in setOf("pdf") -> ResourceType.PDF
            entry.extension.lowercase() in setOf("mp4", "avi", "mkv", "mov", "wmv") -> ResourceType.VIDEO
            entry.extension.lowercase() in setOf("zip", "rar", "7z", "cbz", "cbr") -> ResourceType.ARCHIVE
            else -> return null // Skip unsupported file types
        }

        val organizationMode = if (type == ResourceType.FOLDER) {
            detectOrganizationModeUseCase(fileSource, fullPath)
        } else {
            null
        }

        return ResourceEntity(
            id = UUID.randomUUID().toString(),
            sourceId = source.id,
            name = entry.name,
            type = type,
            organizationMode = organizationMode,
            relativePath = fullPath,
            fileSize = entry.size,
            isAvailable = true,
            lastScannedAt = System.currentTimeMillis(),
        )
    }
}
