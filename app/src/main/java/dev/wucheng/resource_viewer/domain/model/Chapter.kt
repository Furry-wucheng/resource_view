package dev.wucheng.resource_viewer.domain.model

data class Chapter(
    val name: String,
    val relativePath: String,
    val fileCount: Int = 0,
    val coverPath: String? = null,
)
