package dev.wucheng.resource_viewer.data.local.entity

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceEntityTest {

    @Test
    fun `should convert ResourceEntity to Resource domain model`() {
        // Given
        val entity = ResourceEntity(
            id = "resource-1",
            sourceId = "source-1",
            name = "Test Resource",
            type = ResourceType.FOLDER,
            organizationMode = OrganizationMode.FLATGRID,
            relativePath = "/images/test.jpg",
            thumbnailPath = "/thumbnails/test_thumb.jpg",
            fileCount = 1,
            fileSize = 1024L,
            isAvailable = true,
            lastScannedAt = 1234567890L,
            createdAt = 1000000000L,
            updatedAt = 2000000000L,
        )
        val sourceName = "Test Source"
        val tags = listOf(
            Tag(id = "tag-1", name = "Nature", color = "#4CAF50", createdAt = 1000000000L, updatedAt = 1000000000L),
            Tag(id = "tag-2", name = "Landscape", color = "#2196F3", createdAt = 1000000000L, updatedAt = 1000000000L),
        )

        // When
        val resource = entity.toDomain(sourceName, tags)

        // Then
        assertEquals("resource-1", resource.id)
        assertEquals("source-1", resource.sourceId)
        assertEquals("Test Source", resource.sourceName)
        assertEquals("Test Resource", resource.name)
        assertEquals(ResourceType.FOLDER, resource.type)
        assertEquals(OrganizationMode.FLATGRID, resource.organizationMode)
        assertEquals("/images/test.jpg", resource.relativePath)
        assertEquals("/thumbnails/test_thumb.jpg", resource.thumbnailPath)
        assertEquals(1, resource.fileCount)
        assertEquals(1024L, resource.fileSize)
        assertTrue(resource.isAvailable)
        assertEquals(1234567890L, resource.lastScannedAt)
        assertEquals(tags, resource.tags)
        assertEquals(1000000000L, resource.createdAt)
        assertEquals(2000000000L, resource.updatedAt)
    }

    @Test
    fun `should convert ResourceEntity with default empty tags`() {
        // Given
        val entity = ResourceEntity(
            id = "resource-2",
            sourceId = "source-1",
            name = "Minimal Resource",
            type = ResourceType.PDF,
            relativePath = "/docs/test.pdf",
        )

        // When
        val resource = entity.toDomain("Test Source")

        // Then
        assertEquals("resource-2", resource.id)
        assertEquals("Test Source", resource.sourceName)
        assertEquals(ResourceType.PDF, resource.type)
        assertEquals(emptyList<Tag>(), resource.tags)
    }

    @Test
    fun `should convert ResourceEntity with null optional fields`() {
        // Given
        val entity = ResourceEntity(
            id = "resource-3",
            sourceId = "source-2",
            name = "Archive Resource",
            type = ResourceType.ARCHIVE,
            relativePath = "/archives/test.zip",
        )

        // When
        val resource = entity.toDomain("Another Source")

        // Then
        assertEquals("resource-3", resource.id)
        assertNull(resource.organizationMode)
        assertNull(resource.thumbnailPath)
        assertNull(resource.fileCount)
        assertNull(resource.fileSize)
        assertTrue(resource.isAvailable)
        assertNull(resource.lastScannedAt)
    }

    @Test
    fun `should convert ResourceEntity with VIDEO type`() {
        // Given
        val entity = ResourceEntity(
            id = "resource-4",
            sourceId = "source-1",
            name = "Video Resource",
            type = ResourceType.VIDEO,
            relativePath = "/videos/test.mp4",
            organizationMode = OrganizationMode.CHAPTER,
            fileCount = 10,
            fileSize = 1048576L,
        )

        // When
        val resource = entity.toDomain("Test Source")

        // Then
        assertEquals(ResourceType.VIDEO, resource.type)
        assertEquals(OrganizationMode.CHAPTER, resource.organizationMode)
        assertEquals(10, resource.fileCount)
        assertEquals(1048576L, resource.fileSize)
        assertEquals(10, resource.fileCount)
        assertEquals(1048576L, resource.fileSize)
    }
}
