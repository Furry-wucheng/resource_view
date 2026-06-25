package dev.wucheng.resource_viewer.domain.model

data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val isBuiltIn: Boolean = false,
    val resourceCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)
