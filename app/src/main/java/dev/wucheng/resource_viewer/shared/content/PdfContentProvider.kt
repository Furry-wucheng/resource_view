package dev.wucheng.resource_viewer.shared.content

import android.content.Context
import android.graphics.Bitmap
import dev.wucheng.resource_viewer.data.remote.pdf.PdfRenderer
import dev.wucheng.resource_viewer.shared.filesource.FileSource

/**
 * PDF 内容提供者。
 * 使用 PdfRenderer 渲染 PDF 页面，实现 ContentProvider 接口。
 *
 * 注意：此实现遵循 doc/mvp/M22-pdf-viewer.md 中的 M22.2 子任务。
 */
class PdfContentProvider(
    context: Context,
    private val fileSource: FileSource,
    private val relativePath: String,
) : ContentProvider {
    private val renderer: PdfRenderer
    private var isDisposed = false

    init {
        val pdfBytes = kotlinx.coroutines.runBlocking {
            fileSource.readFile(relativePath)
        }
        renderer = PdfRenderer(context, pdfBytes)
    }

    /** PDF 总页数 */
    override val pageCount: Int
        get() {
            checkNotDisposed()
            return renderer.pageCount
        }

    /**
     * 加载指定页并按目标尺寸渲染。
     * @param index 页码（0-based）
     * @param targetWidth 目标宽度（px）
     * @param targetHeight 目标高度（px）
     * @return 按目标尺寸解码的 Bitmap
     */
    override suspend fun loadPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        checkNotDisposed()
        return renderer.renderPage(index, targetWidth, targetHeight)
    }

    /**
     * 释放资源。
     */
    override fun dispose() {
        if (!isDisposed) {
            renderer.close()
            isDisposed = true
        }
    }

    private fun checkNotDisposed() {
        check(!isDisposed) { "PdfContentProvider has been disposed" }
    }
}
