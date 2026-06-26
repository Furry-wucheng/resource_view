package dev.wucheng.resource_viewer.data.local.entity

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceEntityTest {

    @Test
    fun `should convert SourceEntity to Source domain model`() {
        // Given
        val entity = SourceEntity(
            id = "source-1",
            name = "Test Source",
            type = SourceType.LOCAL,
            rootPath = "/test/path",
            host = "192.168.1.1",
            port = 8080,
            username = "user",
            passwordStored = true,
            domain = "test.com",
            enabled = true,
            isAvailable = true,
            lastCheckAt = 1234567890L,
            createdAt = 1000000000L,
            updatedAt = 2000000000L,
        )

        // When
        val source = entity.toDomain()

        // Then
        assertEquals("source-1", source.id)
        assertEquals("Test Source", source.name)
        assertEquals(SourceType.LOCAL, source.type)
        assertEquals("/test/path", source.rootPath)
        assertEquals("192.168.1.1", source.host)
        assertEquals(8080, source.port)
        assertEquals("user", source.username)
        assertTrue(source.passwordStored)
        assertEquals("test.com", source.domain)
        assertTrue(source.enabled)
        assertTrue(source.isAvailable)
        assertEquals(1234567890L, source.lastCheckAt)
        assertEquals(1000000000L, source.createdAt)
        assertEquals(2000000000L, source.updatedAt)
    }

    @Test
    fun `should convert SourceEntity with null optional fields`() {
        // Given
        val entity = SourceEntity(
            id = "source-2",
            name = "Minimal Source",
            type = SourceType.SMB,
            rootPath = "/minimal",
        )

        // When
        val source = entity.toDomain()

        // Then
        assertEquals("source-2", source.id)
        assertEquals("Minimal Source", source.name)
        assertEquals(SourceType.SMB, source.type)
        assertEquals("/minimal", source.rootPath)
        assertNull(source.host)
        assertNull(source.port)
        assertNull(source.username)
        assertFalse(source.passwordStored)
        assertNull(source.domain)
        assertTrue(source.enabled)
        assertFalse(source.isAvailable)
        assertNull(source.lastCheckAt)
    }

    @Test
    fun `should convert SourceEntity with WebDAV type`() {
        // Given
        val entity = SourceEntity(
            id = "source-3",
            name = "WebDAV Source",
            type = SourceType.WEBDAV,
            rootPath = "/webdav",
            host = "webdav.example.com",
            port = 443,
        )

        // When
        val source = entity.toDomain()

        // Then
        assertEquals(SourceType.WEBDAV, source.type)
        assertEquals("webdav.example.com", source.host)
        assertEquals(443, source.port)
    }
}
