package dev.wucheng.resource_viewer.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagEntityTest {

    @Test
    fun `should convert TagEntity to Tag domain model`() {
        // Given
        val entity = TagEntity(
            id = "tag-1",
            name = "Nature",
            color = "#4CAF50",
            isBuiltIn = false,
            createdAt = 1000000000L,
            updatedAt = 2000000000L,
        )

        // When
        val tag = entity.toDomain(5)

        // Then
        assertEquals("tag-1", tag.id)
        assertEquals("Nature", tag.name)
        assertEquals("#4CAF50", tag.color)
        assertFalse(tag.isBuiltIn)
        assertEquals(5, tag.resourceCount)
        assertEquals(1000000000L, tag.createdAt)
        assertEquals(2000000000L, tag.updatedAt)
    }

    @Test
    fun `should convert TagEntity with default resourceCount`() {
        // Given
        val entity = TagEntity(
            id = "tag-2",
            name = "Landscape",
            color = "#2196F3",
        )

        // When
        val tag = entity.toDomain()

        // Then
        assertEquals("tag-2", tag.id)
        assertEquals("Landscape", tag.name)
        assertEquals(0, tag.resourceCount)
    }

    @Test
    fun `should convert built-in TagEntity`() {
        // Given
        val entity = TagEntity(
            id = "builtin-tag",
            name = "Favorites",
            color = "#FF9800",
            isBuiltIn = true,
        )

        // When
        val tag = entity.toDomain(10)

        // Then
        assertEquals("builtin-tag", tag.id)
        assertEquals("Favorites", tag.name)
        assertTrue(tag.isBuiltIn)
        assertEquals(10, tag.resourceCount)
    }
}
