package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import java.util.UUID

/**
 * 拆分资源用例。
 *
 * 接收原 Resource + ResourcePicker 勾选的子项 → 为每个子项创建新 Resource
 * → 标记原 Resource 状态（isAvailable = false）。
 *
 * 注意：此实现遵循 doc/share/06-error-handling.md 中的 ScanResult 定义。
 */
class SplitResourceUseCase(
    private val resourceRepository: ResourceRepository,
) {
    /**
     * 拆分资源。
     *
     * @param parentResource 要拆分的原资源
     * @param selectedItems 勾选的子项列表
     * @param inheritTags 是否继承原资源的标签
     * @return ScanResult 包含成功/跳过/失败统计
     */
    suspend operator fun invoke(
        parentResource: Resource,
        selectedItems: List<FileEntry>,
        inheritTags: Boolean,
    ): Result<ScanResult> {
        if (selectedItems.isEmpty()) {
            return Result.Ok(ScanResult(0, 0, emptyList()))
        }

        var successCount = 0
        val failures = mutableListOf<Pair<String, DomainError>>()
        val batch = mutableListOf<ResourceEntity>()

        for (item in selectedItems) {
            try {
                val type = when {
                    item.isDirectory -> ResourceType.FOLDER
                    item.extension.lowercase() in setOf("pdf") -> ResourceType.PDF
                    item.extension.lowercase() in setOf("mp4", "avi", "mkv", "mov", "wmv") -> ResourceType.VIDEO
                    item.extension.lowercase() in setOf("zip", "rar", "7z", "cbz", "cbr") -> ResourceType.ARCHIVE
                    else -> continue // Skip unsupported types
                }

                val entity = ResourceEntity(
                    id = UUID.randomUUID().toString(),
                    sourceId = parentResource.sourceId,
                    name = item.name,
                    type = type,
                    organizationMode = null, // Will be detected later
                    relativePath = item.relativePath,
                    fileSize = item.size,
                    isAvailable = true,
                    lastScannedAt = System.currentTimeMillis(),
                )
                batch.add(entity)
                successCount++
            } catch (e: Exception) {
                failures.add(item.name to DomainError.DatabaseError("Failed to create resource: ${item.name}", e))
            }
        }

        // Batch insert child resources
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

        // Mark parent resource as split (unavailable)
        if (successCount > 0) {
            val parentEntity = ResourceEntity(
                id = parentResource.id,
                sourceId = parentResource.sourceId,
                name = parentResource.name,
                type = parentResource.type,
                organizationMode = parentResource.organizationMode,
                relativePath = parentResource.relativePath,
                thumbnailPath = parentResource.thumbnailPath,
                fileCount = parentResource.fileCount,
                fileSize = parentResource.fileSize,
                isAvailable = false, // Mark as split
                lastScannedAt = parentResource.lastScannedAt,
                createdAt = parentResource.createdAt,
                updatedAt = System.currentTimeMillis(),
            )
            resourceRepository.update(parentEntity)
        }

        return Result.Ok(ScanResult(successCount, 0, failures))
    }
}
