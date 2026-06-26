package dev.wucheng.resource_viewer.data.local.entity

import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppConfigEntityTest {

    @Test
    fun `should convert AppConfigEntity to AppConfig domain model`() {
        // Given
        val entity = AppConfigEntity(
            id = 1,
            themeMode = ThemeMode.DARK,
            pageDirection = PageDirection.LEFT_TO_RIGHT,
            doublePageMode = DoublePageMode.SINGLE,
            crossChapter = false,
            cacheLimitMB = 1000,
            thumbnailConcurrency = 8,
            autoSyncInterval = AutoSyncInterval.HOUR_1,
            updatedAt = 1234567890L,
            hasAcceptedPrivacy = true,
        )

        // When
        val config = entity.toDomain()

        // Then
        assertEquals(ThemeMode.DARK, config.themeMode)
        assertEquals(PageDirection.LEFT_TO_RIGHT, config.pageDirection)
        assertEquals(DoublePageMode.SINGLE, config.doublePageMode)
        assertEquals(false, config.crossChapter)
        assertEquals(1000, config.cacheLimitMB)
        assertEquals(8, config.thumbnailConcurrency)
        assertEquals(AutoSyncInterval.HOUR_1, config.autoSyncInterval)
    }

    @Test
    fun `should convert AppConfigEntity with default values`() {
        // Given
        val entity = AppConfigEntity()

        // When
        val config = entity.toDomain()

        // Then
        assertEquals(ThemeMode.SYSTEM, config.themeMode)
        assertEquals(PageDirection.RIGHT_TO_LEFT, config.pageDirection)
        assertEquals(DoublePageMode.AUTO, config.doublePageMode)
        assertEquals(true, config.crossChapter)
        assertEquals(500, config.cacheLimitMB)
        assertEquals(4, config.thumbnailConcurrency)
        assertNull(config.autoSyncInterval)
    }

    @Test
    fun `should convert AppConfigEntity with null autoSyncInterval`() {
        // Given
        val entity = AppConfigEntity(
            autoSyncInterval = null,
        )

        // When
        val config = entity.toDomain()

        // Then
        assertNull(config.autoSyncInterval)
    }
}
