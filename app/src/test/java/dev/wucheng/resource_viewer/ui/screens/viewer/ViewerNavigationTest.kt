package dev.wucheng.resource_viewer.ui.screens.viewer

import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerNavigationTest {
    @Test
    fun `ltr tap zones map left to previous and right to next`() {
        assertEquals(ViewerTapAction.PREVIOUS, resolveTapAction(10f, 50f, 300f, 150f, PageDirection.LEFT_TO_RIGHT))
        assertEquals(ViewerTapAction.NEXT, resolveTapAction(290f, 50f, 300f, 150f, PageDirection.LEFT_TO_RIGHT))
    }

    @Test
    fun `rtl tap zones reverse left and right`() {
        assertEquals(ViewerTapAction.NEXT, resolveTapAction(10f, 50f, 300f, 150f, PageDirection.RIGHT_TO_LEFT))
        assertEquals(ViewerTapAction.PREVIOUS, resolveTapAction(290f, 50f, 300f, 150f, PageDirection.RIGHT_TO_LEFT))
    }

    @Test
    fun `vertical tap zones use top and bottom`() {
        assertEquals(ViewerTapAction.PREVIOUS, resolveTapAction(50f, 10f, 150f, 300f, PageDirection.VERTICAL))
        assertEquals(ViewerTapAction.NEXT, resolveTapAction(50f, 290f, 150f, 300f, PageDirection.VERTICAL))
    }

    @Test
    fun `center tap toggles toolbar`() {
        assertEquals(ViewerTapAction.TOGGLE_TOOLBAR, resolveTapAction(150f, 75f, 300f, 150f, PageDirection.LEFT_TO_RIGHT))
    }
}
