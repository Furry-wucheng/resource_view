package dev.wucheng.resource_viewer.data.remote.pdf

import android.content.Context
import android.graphics.Bitmap
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import java.io.Closeable
import java.io.IOException

/**
 * PDF 渲染工具类。
 * 封装 pdfium-android 的 PdfiumCore，提供简洁的 PDF 渲染 API。
 *
 * 注意：此实现遵循 doc/mvp/M22-pdf-viewer.md 中的 M22.1 子任务。
 */
class PdfRenderer(
    context: Context,
    pdfBytes: ByteArray,
) : Closeable {
    private val pdfiumCore = PdfiumCore(context)
    private val document: PdfDocument
    private var isClosed = false

    init {
        try {
            document = pdfiumCore.newDocument(pdfBytes)
        } catch (e: IOException) {
            throw IOException("Failed to open PDF document", e)
        }
    }

    /** PDF 总页数 */
    val pageCount: Int
        get() {
            checkNotClosed()
            return document.getPageCount()
        }

    /**
     * 渲染指定页为 Bitmap。
     * @param pageIndex 页码（0-based）
     * @param width 目标宽度（px）
     * @param height 目标高度（px）
     * @return 渲染后的 Bitmap
     * @throws IllegalArgumentException 如果页码无效或尺寸非法
     */
    fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap {
        checkNotClosed()
        require(pageIndex in 0 until pageCount) { "Page index $pageIndex out of range [0, $pageCount)" }
        require(width > 0) { "Width must be positive, got $width" }
        require(height > 0) { "Height must be positive, got $height" }

        val page = document.openPage(pageIndex)
            ?: throw IllegalStateException("Failed to open page $pageIndex")
        return page.use { p ->
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            p.renderPageBitmap(bitmap, 0, 0, width, height)
            bitmap
        }
    }

    /**
     * 获取指定页的原始尺寸（PDF 点）。
     * @param pageIndex 页码（0-based）
     * @return Pair(width, height) in PDF points
     */
    fun getPageSize(pageIndex: Int): Pair<Int, Int> {
        checkNotClosed()
        require(pageIndex in 0 until pageCount) { "Page index $pageIndex out of range [0, $pageCount)" }

        val page = document.openPage(pageIndex)
            ?: throw IllegalStateException("Failed to open page $pageIndex")
        return page.use { p ->
            Pair(p.getPageWidthPoint(), p.getPageHeightPoint())
        }
    }

    /**
     * 释放资源。
     */
    override fun close() {
        if (!isClosed) {
            document.close()
            isClosed = true
        }
    }

    private fun checkNotClosed() {
        check(!isClosed) { "PdfRenderer has been closed" }
    }
}
