package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import dev.wucheng.resource_viewer.shared.content.ImageFolderProvider
import dev.wucheng.resource_viewer.shared.filesource.FileSource

/**
 * 画廊组织策略。
 * 所有图片文件以画廊风格展示，无章节概念。
 *
 * 注意：此实现遵循 doc/mvp/M20-gallery-flatgrid-strategies.md 中的 M20.2 子任务。
 * 与 FlatGridStrategy 的区别在于 UI 层的呈现方式（画廊风格布局）。
 */
class GalleryStrategy : OrganizationStrategy {
    /** 支持的图片扩展名 */
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

    override val mode: OrganizationMode = OrganizationMode.GALLERY

    /**
     * 获取章节列表 - Gallery 模式无章节，返回空列表。
     */
    override suspend fun getChapters(resource: Resource, fileSource: FileSource): List<Chapter> {
        return emptyList()
    }

    /**
     * 获取内容列表 - 返回文件夹内所有图片文件。
     */
    override suspend fun getContents(resource: Resource, fileSource: FileSource): List<FileEntry> {
        return collectImages(fileSource, resource.relativePath)
    }

    private suspend fun collectImages(fileSource: FileSource, path: String): List<FileEntry> {
        val entries = fileSource.listDirectory(path)
        return entries.flatMap { entry ->
            when {
                entry.isDirectory -> collectImages(fileSource, entry.relativePath)
                entry.extension.lowercase() in imageExtensions -> listOf(entry)
                else -> emptyList()
            }
        }
    }

    /**
     * 创建内容提供者 - 使用 ImageFolderProvider。
     */
    override fun createProvider(
        resource: Resource,
        fileSource: FileSource,
        chapter: Chapter?,
    ): ContentProvider {
        return ImageFolderProvider(fileSource, resource.relativePath, recursive = true)
    }
}
