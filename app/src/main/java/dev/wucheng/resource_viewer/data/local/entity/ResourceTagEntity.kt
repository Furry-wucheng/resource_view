package dev.wucheng.resource_viewer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "resource_tags",
    primaryKeys = ["resourceId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["resourceId", "tagId"]),
        Index(value = ["tagId", "resourceId"]),
    ],
)
data class ResourceTagEntity(
    val resourceId: String,
    val tagId: String,
    val createdAt: Long = System.currentTimeMillis(),
)
