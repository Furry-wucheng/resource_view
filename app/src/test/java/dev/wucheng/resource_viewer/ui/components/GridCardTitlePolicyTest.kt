package dev.wucheng.resource_viewer.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class GridCardTitlePolicyTest {
    @Test
    fun `grid card title should use one line before ellipsis`() {
        assertEquals(1, GRID_CARD_TITLE_MAX_LINES)
    }
}
