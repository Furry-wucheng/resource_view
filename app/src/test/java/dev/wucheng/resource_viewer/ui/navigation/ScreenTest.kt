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

    @Test
    fun `should create chapter viewer route with encoded path`() {
        val route = Screen.ChapterViewer.createRoute("resource-1", "book/第 1 章")

        assertEquals("viewer/resource-1/chapter?path=book%2F%E7%AC%AC+1+%E7%AB%A0", route)
    }

    @Test
    fun `should create sequence viewer route with initial page`() {
        val route = Screen.SequenceViewer.createRoute("resource-1", "book/pages", 7)

        assertEquals("viewer/resource-1/sequence?path=book%2Fpages&page=7", route)
    }
}
