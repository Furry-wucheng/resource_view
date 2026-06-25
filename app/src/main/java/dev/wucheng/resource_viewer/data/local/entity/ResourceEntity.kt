package dev.wucheng.resource_viewer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType

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
