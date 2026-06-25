package dev.wucheng.resource_viewer.shared.organization

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import dev.wucheng.resource_viewer.shared.filesource.FileSource

/**
 * 组织模式策略接口。
 * 每种模式实现自己的章节拆解和内容获取逻辑。
 *
 * 注意：此接口定义来自 doc/share/02-interfaces.md 共享契约。
 * 实现类将在后续 Stage 中添加：
 * - FlatGridStrategy (M20)
 * - GalleryStrategy (M20)
 * - ChapterStrategy (M21)
 * - ChapterGalleryStrategy (M21)
 */
interface OrganizationStrategy {
    val mode: OrganizationMode

    /** 获取章节列表（CHAPTER/CHAPTER_GALLERY 模式） */
    suspend fun getChapters(resource: Resource, fileSource: FileSource): List<Chapter>

    /** 获取内容列表。GALLERY 模式返回递归展开的全部文件，大数据量时用 Sequence 懒遍历。 */
    suspend fun getContents(resource: Resource, fileSource: FileSource): List<FileEntry>

    /** 为指定章节创建 ContentProvider */
    fun createProvider(
        resource: Resource,
        fileSource: FileSource,
        chapter: Chapter? = null,
    ): ContentProvider
}
