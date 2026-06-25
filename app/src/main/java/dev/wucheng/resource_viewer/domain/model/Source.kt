package dev.wucheng.resource_viewer.domain.model

import dev.wucheng.resource_viewer.data.local.converter.SourceType

data class Source(
    val id: String,
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
    val createdAt: Long,
    val updatedAt: Long,
)
