package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import dev.wucheng.resource_viewer.shared.content.ImageFolderProvider
import dev.wucheng.resource_viewer.shared.filesource.FileSource

/**
 * 章节组织策略。
 * 子文件夹作为章节，选章后只浏览该章内容。
 *
 * 注意：此实现遵循 doc/mvp/M21-chapter-strategies.md 中的 M21.1 子任务。
 */
class ChapterStrategy : OrganizationStrategy {
    /** 支持的图片扩展名 */
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

    override val mode: OrganizationMode = OrganizationMode.CHAPTER

    /**
     * 获取章节列表 - 列出子文件夹作为章节。
     * 每个章节统计图片数量和封面路径。
     */
    override suspend fun getChapters(resource: Resource, fileSource: FileSource): List<Chapter> {
        val entries = fileSource.listDirectory(resource.relativePath)
        val directories = entries.filter { it.isDirectory }

        return directories.map { dir ->
            val chapterEntries = fileSource.listDirectory(dir.relativePath)
            val imageFiles = chapterEntries.filter {
                !it.isDirectory && it.extension.lowercase() in imageExtensions
            }
            Chapter(
                name = dir.name,
                relativePath = dir.relativePath,
                fileCount = imageFiles.size,
                coverPath = imageFiles.firstOrNull()?.relativePath,
            )
        }
    }

    /**
     * 获取内容列表 - 返回空（由章节控制）。
     */
    override suspend fun getContents(resource: Resource, fileSource: FileSource): List<FileEntry> {
        return emptyList()
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
        requireNotNull(chapter) { "Chapter is required for ChapterStrategy" }
        return ImageFolderProvider(fileSource, chapter.relativePath)
    }
}
