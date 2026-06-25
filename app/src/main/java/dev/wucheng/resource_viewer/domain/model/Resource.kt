package dev.wucheng.resource_viewer.domain.model

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType

data class Resource(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val name: String,
    val type: ResourceType,
    val organizationMode: OrganizationMode?,
    val relativePath: String,
    val thumbnailPath: String?,
    val fileCount: Int?,
    val fileSize: Long?,
    val isAvailable: Boolean,
    val lastScannedAt: Long?,
    val tags: List<Tag> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)
