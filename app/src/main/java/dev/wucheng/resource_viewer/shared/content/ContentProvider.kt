package dev.wucheng.resource_viewer.shared.content

import android.graphics.Bitmap

/**
 * 查看器内容抽象接口。
 * 图片文件夹、PDF、压缩包各自实现此接口。
 *
 * 注意：此接口定义来自 doc/share/02-interfaces.md 共享契约。
 * 实现类将在后续 Stage 中添加：
 * - ImageFolderProvider (M14)
 * - PdfContentProvider (M22)
 * - ArchiveContentProvider (P2)
 */
interface ContentProvider {
    /** 总页数 */
    val pageCount: Int

    /**
     * 加载指定页并按目标尺寸渲染。
     * @param index 页码（0-based）
     * @param targetWidth 目标宽度（px），用于控制解码/渲染分辨率
     * @param targetHeight 目标高度（px）
     * @return 按目标尺寸解码的 Bitmap
     */
    suspend fun loadPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap

    /** 释放资源（文件句柄、PDF 文档等） */
    fun dispose()
}
