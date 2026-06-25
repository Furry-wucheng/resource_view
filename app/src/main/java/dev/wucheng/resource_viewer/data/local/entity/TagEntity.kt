package dev.wucheng.resource_viewer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.wucheng.resource_viewer.domain.model.Tag

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * 将 TagEntity 转换为 Domain Model。
 * @param resourceCount 关联的资源数量，需要从 ResourceTagEntity 统计
 */
fun TagEntity.toDomain(resourceCount: Int = 0): Tag = Tag(
    id = id,
    name = name,
    color = color,
    isBuiltIn = isBuiltIn,
    resourceCount = resourceCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
