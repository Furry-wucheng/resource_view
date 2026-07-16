package dev.wucheng.resource_viewer.ui.screens.viewer

import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoublePageLayoutTest {

    @Test
    fun `should force single page on phone in portrait and landscape`() {
        assertFalse(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.AUTO, 400, 900))
        assertFalse(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.AUTO, 899, 400))
        assertFalse(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.DOUBLE, 899, 400))
    }

    @Test
    fun `should force single page for vertical paging on wide screen`() {
        assertFalse(shouldUseDoublePage(PageDirection.VERTICAL, DoublePageMode.DOUBLE, 1200, 800))
    }

    @Test
    fun `should respect double page settings on wide screen`() {
        assertTrue(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.DOUBLE, 900, 1200))
        assertFalse(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.SINGLE, 1200, 800))
        assertTrue(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.AUTO, 1200, 800))
        assertFalse(shouldUseDoublePage(PageDirection.RIGHT_TO_LEFT, DoublePageMode.AUTO, 900, 1200))
    }
}
