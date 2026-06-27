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
 * 章节画廊组织策略。
 * 根层子文件夹作为章节，章内递归扁平所有图片（跨子文件夹连续阅读）。
 *
 * 注意：此实现遵循 doc/mvp/M21-chapter-strategies.md 中的 M21.2 子任务。
 */
class ChapterGalleryStrategy : OrganizationStrategy {
    /** 支持的图片扩展名 */
    private val imageExtensions = MediaFormats.imageExtensions

    override val mode: OrganizationMode = OrganizationMode.CHAPTER_GALLERY

    /**
     * 获取章节列表 - 根层子文件夹作为章节。
     * 每个章节递归统计图片数量和封面路径。
     */
    override suspend fun getChapters(resource: Resource, fileSource: FileSource): List<Chapter> {
        val entries = fileSource.listDirectory(resource.relativePath)
        val directories = entries.filter { it.isDirectory }

        return directories.map { dir ->
            val allImages = collectImagesRecursive(fileSource, dir.relativePath)
            Chapter(
                name = dir.name,
                relativePath = dir.relativePath,
                fileCount = allImages.size,
                coverPath = allImages.firstOrNull()?.relativePath,
            )
        }
    }

    /**
     * 获取内容列表 - 返回整个资源目录下所有图片（递归扁平）。
     * 用于跨子文件夹连续阅读。
     */
    override suspend fun getContents(resource: Resource, fileSource: FileSource): List<FileEntry> {
        return collectImagesRecursive(fileSource, resource.relativePath)
    }

    /**
     * 创建内容提供者 - 为特定章节创建 ImageFolderProvider。
     * @param chapter 必须指定章节
     */
    override fun createProvider(
        resource: Resource,
        fileSource: FileSource,
        chapter: Chapter?,
    ): ContentProvider {
        requireNotNull(chapter) { "Chapter is required for ChapterGalleryStrategy" }
        return ImageFolderProvider(fileSource, chapter.relativePath, recursive = true)
    }

    /**
     * 递归收集目录下所有图片文件。
     */
    private suspend fun collectImagesRecursive(
        fileSource: FileSource,
        directoryPath: String,
    ): List<FileEntry> {
        val entries = fileSource.listDirectory(directoryPath)
        val result = mutableListOf<FileEntry>()

        for (entry in entries) {
            if (entry.isDirectory) {
                // 递归进入子目录
                result.addAll(collectImagesRecursive(fileSource, entry.relativePath))
            } else if (entry.extension.lowercase() in imageExtensions) {
                result.add(entry)
            }
        }

        return result
    }
}
