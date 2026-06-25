package dev.wucheng.resource_viewer.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    // SourceType
    @Test
    fun `should convert SourceType to string and back`() {
        SourceType.entries.forEach { enum ->
            val string = converters.fromSourceType(enum)
            val result = converters.toSourceType(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to SourceType`() {
        assertNull(converters.toSourceType(null))
    }

    @Test
    fun `should return null when converting invalid string to SourceType`() {
        assertNull(converters.toSourceType("INVALID"))
    }

    // ResourceType
    @Test
    fun `should convert ResourceType to string and back`() {
        ResourceType.entries.forEach { enum ->
            val string = converters.fromResourceType(enum)
            val result = converters.toResourceType(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to ResourceType`() {
        assertNull(converters.toResourceType(null))
    }

    @Test
    fun `should return null when converting invalid string to ResourceType`() {
        assertNull(converters.toResourceType("INVALID"))
    }

    // OrganizationMode
    @Test
    fun `should convert OrganizationMode to string and back`() {
        OrganizationMode.entries.forEach { enum ->
            val string = converters.fromOrganizationMode(enum)
            val result = converters.toOrganizationMode(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to OrganizationMode`() {
        assertNull(converters.toOrganizationMode(null))
    }

    @Test
    fun `should return null when converting invalid string to OrganizationMode`() {
        assertNull(converters.toOrganizationMode("INVALID"))
    }

    // ThemeMode
    @Test
    fun `should convert ThemeMode to string and back`() {
        ThemeMode.entries.forEach { enum ->
            val string = converters.fromThemeMode(enum)
            val result = converters.toThemeMode(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to ThemeMode`() {
        assertNull(converters.toThemeMode(null))
    }

    // PageDirection
    @Test
    fun `should convert PageDirection to string and back`() {
        PageDirection.entries.forEach { enum ->
            val string = converters.fromPageDirection(enum)
            val result = converters.toPageDirection(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to PageDirection`() {
        assertNull(converters.toPageDirection(null))
    }

    // DoublePageMode
    @Test
    fun `should convert DoublePageMode to string and back`() {
        DoublePageMode.entries.forEach { enum ->
            val string = converters.fromDoublePageMode(enum)
            val result = converters.toDoublePageMode(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to DoublePageMode`() {
        assertNull(converters.toDoublePageMode(null))
    }

    // AutoSyncInterval
    @Test
    fun `should convert AutoSyncInterval to string and back`() {
        AutoSyncInterval.entries.forEach { enum ->
            val string = converters.fromAutoSyncInterval(enum)
            val result = converters.toAutoSyncInterval(string)
            assertEquals(enum, result)
        }
    }

    @Test
    fun `should return null when converting null string to AutoSyncInterval`() {
        assertNull(converters.toAutoSyncInterval(null))
    }
}
