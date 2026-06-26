package dev.wucheng.resource_viewer.ui.navigation

import org.junit.Test
import org.junit.Assert.assertEquals

class ScreenTest {

    @Test
    fun `should have correct route for Home`() {
        // Given
        val screen = Screen.Home

        // Then
        assertEquals("home", screen.route)
    }

    @Test
    fun `should have correct route for Sources`() {
        // Given
        val screen = Screen.Sources

        // Then
        assertEquals("sources", screen.route)
    }

    @Test
    fun `should have correct route for Settings`() {
        // Given
        val screen = Screen.Settings

        // Then
        assertEquals("settings", screen.route)
    }

    @Test
    fun `should have correct route for Viewer with parameter`() {
        // Given
        val screen = Screen.Viewer

        // Then
        assertEquals("viewer/{resourceId}", screen.route)
    }

    @Test
    fun `should create correct route for Viewer with actual resourceId`() {
        // Given
        val resourceId = "test-resource-123"

        // When
        val route = Screen.Viewer.createRoute(resourceId)

        // Then
        assertEquals("viewer/test-resource-123", route)
    }

    @Test
    fun `should have correct route for TagManager`() {
        // Given
        val screen = Screen.TagManager

        // Then
        assertEquals("tags/manager", screen.route)
    }

    // === M16: 底部标签栏路由测试 ===

    @Test
    fun `should have correct route for Knowledge`() {
        // Given
        val screen = Screen.Knowledge

        // Then
        assertEquals("knowledge", screen.route)
    }

    @Test
    fun `should have correct route for Toolbox`() {
        // Given
        val screen = Screen.Toolbox

        // Then
        assertEquals("toolbox", screen.route)
    }
}
