package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import dev.wucheng.resource_viewer.shared.content.ImageFolderProvider
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats

/**
 * 平铺网格组织策略。
 * 所有图片文件平铺显示，无章节概念。
 *
 * 注意：此实现遵循 doc/mvp/M20-gallery-flatgrid-strategies.md 中的 M20.1 子任务。
 */
class FlatGridStrategy : OrganizationStrategy {
    /** 支持的图片扩展名 */
    private val imageExtensions = MediaFormats.imageExtensions
    /** 支持的视频扩展名 */
    private val videoExtensions = MediaFormats.videoExtensions

    override val mode: OrganizationMode = OrganizationMode.FLATGRID

    override suspend fun getChapters(resource: Resource, fileSource: FileSource): List<Chapter> {
        return emptyList()
    }

    override suspend fun getContents(resource: Resource, fileSource: FileSource): List<FileEntry> {
        val entries = fileSource.listDirectory(resource.relativePath)
        return entries.filter { entry ->
            !entry.isDirectory && entry.extension.lowercase() in (imageExtensions + videoExtensions)
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
        return ImageFolderProvider(fileSource, resource.relativePath)
    }
}
