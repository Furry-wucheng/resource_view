package dev.wucheng.resource_viewer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag

@Entity(
    tableName = "resources",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["sourceId", "relativePath"], unique = true),
        Index(value = ["createdAt", "id"]),
        Index(value = ["name", "id"]),
    ],
)
data class ResourceEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val name: String,
    val type: ResourceType,
    val organizationMode: OrganizationMode? = null,
    val relativePath: String,
    val thumbnailPath: String? = null,
    val fileCount: Int? = null,
    val fileSize: Long? = null,
    val isAvailable: Boolean = true,
    val lastScannedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * 将 ResourceEntity 转换为 Domain Model。
 * @param sourceName 来源名称，需要从 SourceEntity 查询获取
 * @param tags 关联的标签列表，需要从 TagEntity 查询获取
 */
fun ResourceEntity.toDomain(
    sourceName: String,
    tags: List<Tag> = emptyList(),
): Resource = Resource(
    id = id,
    sourceId = sourceId,
    sourceName = sourceName,
    name = name,
    type = type,
    organizationMode = organizationMode,
    relativePath = relativePath,
    thumbnailPath = thumbnailPath,
    fileCount = fileCount,
    fileSize = fileSize,
    isAvailable = isAvailable,
    lastScannedAt = lastScannedAt,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
