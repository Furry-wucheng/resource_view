package dev.wucheng.resource_viewer.domain.usecase

import android.content.Context
import android.util.Log
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.ThumbnailRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailTaskPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class SplitResourceUseCase(
    private val resourceRepository: ResourceRepository,
    private val detectOrganizationModeUseCase: DetectOrganizationModeUseCase = DetectOrganizationModeUseCase(),
    private val thumbnailRepository: ThumbnailRepository? = null,
    private val context: Context? = null,
    private val database: AppDatabase? = null,
) {
    suspend operator fun invoke(
        parentResource: Resource,
        selectedItems: List<FileEntry>,
        fileSource: FileSource,
        deleteOriginal: Boolean = false,
        inheritTags: Boolean = false,
        organizationMode: OrganizationMode? = null,
        tagIds: List<String> = emptyList(),
        thumbnailScope: CoroutineScope? = null,
    ): Result<ScanResult> {
        if (selectedItems.isEmpty()) {
            return Result.Ok(ScanResult(0, 0, emptyList()))
        }

        val existingPaths = try {
            resourceRepository.getVisibleResources().first()
                .filter { it.sourceId == parentResource.sourceId }
                .map { it.relativePath }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }

        var successCount = 0
        var skipCount = 0
        val failures = mutableListOf<Pair<String, DomainError>>()
        val batch = mutableListOf<ResourceEntity>()
        val childIds = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (item in selectedItems) {
            try {
                if (item.relativePath in seen || item.relativePath in existingPaths) {
                    skipCount++
                    continue
                }
                seen.add(item.relativePath)

                val type = when {
                    item.isDirectory -> ResourceType.FOLDER
                    item.extension.lowercase() in setOf("pdf") -> ResourceType.PDF
                    item.extension.lowercase() in setOf("mp4", "avi", "mkv", "mov", "wmv") -> ResourceType.VIDEO
                    item.extension.lowercase() in setOf("zip", "rar", "7z", "cbz", "cbr") -> ResourceType.ARCHIVE
                    else -> { skipCount++; continue }
                }

                val orgMode = when {
                    organizationMode != null -> organizationMode
                    type == ResourceType.FOLDER -> detectOrganizationModeUseCase(fileSource, item.relativePath)
                    else -> null
                }

                val childId = UUID.randomUUID().toString()
                val entity = ResourceEntity(
                    id = childId,
                    sourceId = parentResource.sourceId,
                    name = item.name,
                    type = type,
                    organizationMode = orgMode,
                    relativePath = item.relativePath,
                    fileSize = item.size,
                    isAvailable = true,
                    lastScannedAt = System.currentTimeMillis(),
                )
                batch.add(entity)
                childIds.add(childId)
                successCount++
            } catch (e: Exception) {
                failures.add(item.name to DomainError.DatabaseError("Failed to create resource: ${item.name}", e))
            }
        }

        if (batch.isNotEmpty()) {
            when (val insertResult = resourceRepository.insertAll(batch)) {
                is Result.Ok -> {
                    // Tag assignment
                    if (tagIds.isNotEmpty()) {
                        for (childId in childIds) {
                            resourceRepository.setResourceTags(childId, tagIds)
                        }
                    } else if (inheritTags) {
                        val parentTags = parentResource.tags.map { it.id }
                        if (parentTags.isNotEmpty()) {
                            for (childId in childIds) {
                                resourceRepository.setResourceTags(childId, parentTags)
                            }
                        }
                    }

                    // Thumbnail generation (async, non-blocking)
                    if (thumbnailScope != null) {
                        thumbnailScope.launch(Dispatchers.IO) {
                            generateThumbnails(batch, fileSource)
                        }
                    } else {
                        generateThumbnails(batch, fileSource)
                    }
                }
                is Result.Err -> {
                    batch.forEach { entity ->
                        failures.add(entity.name to insertResult.error)
                    }
                    successCount = 0
                    return Result.Ok(ScanResult(0, skipCount, failures))
                }
            }
        }

        if (successCount > 0) {
            if (deleteOriginal) {
                resourceRepository.deleteById(parentResource.id)
            } else {
                resourceRepository.update(
                    ResourceEntity(
                        id = parentResource.id,
                        sourceId = parentResource.sourceId,
                        name = parentResource.name,
                        type = parentResource.type,
                        organizationMode = parentResource.organizationMode,
                        relativePath = parentResource.relativePath,
                        thumbnailPath = parentResource.thumbnailPath,
                        fileCount = parentResource.fileCount,
                        fileSize = parentResource.fileSize,
                        isAvailable = false,
                        lastScannedAt = parentResource.lastScannedAt,
                        createdAt = parentResource.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        }

        return Result.Ok(ScanResult(successCount, skipCount, failures))
    }

    private suspend fun generateThumbnails(entities: List<ResourceEntity>, fileSource: FileSource) {
        val thumbRepo = thumbnailRepository ?: return
        val ctx = context ?: return
        val db = database ?: return

        val config = db.appConfigDao().getConfig().first()
        val concurrency = config?.thumbnailConcurrency ?: 4
        val pool = ThumbnailTaskPool(concurrency)
        val cacheDir = ctx.cacheDir.resolve("thumbnails/resources")
        cacheDir.mkdirs()

        coroutineScope {
            entities.map { entity ->
                async {
                    pool.run {
                        try {
                            val resource = entity.toDomain()
                            when (val result = thumbRepo.generateThumbnail(resource, fileSource, cacheDir)) {
                                is Result.Ok -> {
                                    result.value?.let { thumbFile ->
                                        resourceRepository.updateThumbnail(entity.id, thumbFile.absolutePath)
                                    }
                                }
                                is Result.Err -> Log.e(TAG, "Thumbnail failed for ${entity.name}: ${result.error}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Thumbnail exception for ${entity.name}", e)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun ResourceEntity.toDomain(): Resource = Resource(
        id = id, sourceId = sourceId, sourceName = "",
        name = name, type = type, organizationMode = organizationMode,
        relativePath = relativePath, fileSize = fileSize, fileCount = null,
        isAvailable = isAvailable, lastScannedAt = lastScannedAt,
        createdAt = createdAt, updatedAt = updatedAt, thumbnailPath = thumbnailPath,
    )

    private companion object {
        const val TAG = "SplitResourceUseCase"
    }
}
