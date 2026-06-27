package dev.wucheng.resource_viewer.ui.navigation

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceDestinationResolverTest {
    @Test
    fun `chapter modes open chapter list`() {
        assertEquals(ResourceDestination.CHAPTER_LIST, resolveResourceDestination(OrganizationMode.CHAPTER))
        assertEquals(ResourceDestination.CHAPTER_LIST, resolveResourceDestination(OrganizationMode.CHAPTER_GALLERY))
    }

    @Test
    fun `grid modes open their dedicated screens`() {
        assertEquals(ResourceDestination.FLAT_GRID, resolveResourceDestination(OrganizationMode.FLATGRID))
        assertEquals(ResourceDestination.GALLERY, resolveResourceDestination(OrganizationMode.GALLERY))
    }

    @Test
    fun `missing mode falls back to viewer`() {
        assertEquals(ResourceDestination.VIEWER, resolveResourceDestination(null))
    }
}
