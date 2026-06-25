package dev.wucheng.resource_viewer.shared.thumbnail

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import java.io.File

/**
 * 缩略图生成策略接口。
 * 按来源类型选择实现。
 *
 * 注意：此接口定义来自 doc/share/02-interfaces.md 共享契约。
 * 实现类将在后续 Stage 中添加：
 * - ImageThumbnailGenerator (M23)
 * - PdfThumbnailGenerator (M22)
 * - VideoThumbnailGenerator (M19)
 * - ArchiveThumbnailGenerator (P2)
 */
interface ThumbnailGenerator {
    /** 是否可处理该资源类型 */
    fun canHandle(type: ResourceType): Boolean

    /** 生成缩略图，返回缓存文件路径 */
    suspend fun generate(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): File?
}
