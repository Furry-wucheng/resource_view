package dev.wucheng.resource_viewer.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveLayoutTest {

    @Test
    fun `should use compact layout below 900dp`() {
        assertFalse(isWideLayout(899))
        assertTrue(useCompactSourceActions(599f))
    }

    @Test
    fun `should use wide layout from 900dp`() {
        assertTrue(isWideLayout(900))
        assertFalse(useCompactSourceActions(900f))
    }

    @Test
    fun `should stack dialog fields below 480dp content width`() {
        assertTrue(useStackedDialogFields(479f))
        assertFalse(useStackedDialogFields(480f))
    }

    @Test
    fun `should open directory tree initially only on enabled wide layout`() {
        assertFalse(shouldOpenDirectoryTreeInitially(isWide = false, isEnabled = true))
        assertFalse(shouldOpenDirectoryTreeInitially(isWide = true, isEnabled = false))
        assertTrue(shouldOpenDirectoryTreeInitially(isWide = true, isEnabled = true))
    }

    @Test
    fun `should close directory tree after navigation only on compact layout`() {
        assertTrue(shouldCloseDirectoryTreeAfterNavigation(isWide = false))
        assertFalse(shouldCloseDirectoryTreeAfterNavigation(isWide = true))
    }
}
