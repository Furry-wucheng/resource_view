package dev.wucheng.resource_viewer.domain.model

data class FileEntry(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val extension: String = "",
)
