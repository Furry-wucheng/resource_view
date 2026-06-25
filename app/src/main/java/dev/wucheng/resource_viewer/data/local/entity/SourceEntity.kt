package dev.wucheng.resource_viewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.domain.model.Source

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: SourceType,
    val rootPath: String,
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val passwordStored: Boolean = false,
    val domain: String? = null,
    val enabled: Boolean = true,
    val isAvailable: Boolean = false,
    val lastCheckAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

fun SourceEntity.toDomain(): Source = Source(
    id = id,
    name = name,
    type = type,
    rootPath = rootPath,
    host = host,
    port = port,
    username = username,
    passwordStored = passwordStored,
    domain = domain,
    enabled = enabled,
    isAvailable = isAvailable,
    lastCheckAt = lastCheckAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
