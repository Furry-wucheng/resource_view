package dev.wucheng.resource_viewer.ui.navigation

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
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

    @Test
    fun `pdf resource opens viewer regardless of organization mode`() {
        val pdf = Resource(
            id = "pdf-1",
            sourceId = "source-1",
            sourceName = "Source",
            name = "Book.pdf",
            type = ResourceType.PDF,
            organizationMode = OrganizationMode.FLATGRID,
            relativePath = "Book.pdf",
            thumbnailPath = null,
            fileCount = null,
            fileSize = 1024L,
            isAvailable = true,
            lastScannedAt = 0L,
            createdAt = 0L,
            updatedAt = 0L,
        )

        assertEquals(ResourceDestination.VIEWER, resolveResourceDestination(pdf))
    }
}
