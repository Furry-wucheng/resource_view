package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppConfigDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var appConfigDao: AppConfigDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appConfigDao = db.appConfigDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `should return null when no config exists`() = runTest {
        val config = appConfigDao.getConfig().first()
        assertNull(config)
    }

    @Test
    fun `should save and get config`() = runTest {
        val config = AppConfigEntity(
            themeMode = ThemeMode.DARK,
            pageDirection = PageDirection.LEFT_TO_RIGHT,
            doublePageMode = DoublePageMode.DOUBLE,
            crossChapter = false,
            cacheLimitMB = 1000,
            thumbnailConcurrency = 8,
        )
        appConfigDao.save(config)

        val result = appConfigDao.getConfig().first()
        assertNotNull(result)
        assertEquals(ThemeMode.DARK, result?.themeMode)
        assertEquals(PageDirection.LEFT_TO_RIGHT, result?.pageDirection)
        assertEquals(DoublePageMode.DOUBLE, result?.doublePageMode)
        assertEquals(false, result?.crossChapter)
        assertEquals(1000, result?.cacheLimitMB)
        assertEquals(8, result?.thumbnailConcurrency)
    }

    @Test
    fun `should update config on conflict`() = runTest {
        val config1 = AppConfigEntity(themeMode = ThemeMode.LIGHT)
        appConfigDao.save(config1)

        val config2 = AppConfigEntity(themeMode = ThemeMode.DARK)
        appConfigDao.save(config2)

        val result = appConfigDao.getConfig().first()
        assertNotNull(result)
        assertEquals(ThemeMode.DARK, result?.themeMode)
    }

    @Test
    fun `should use default values`() = runTest {
        val config = AppConfigEntity()
        appConfigDao.save(config)

        val result = appConfigDao.getConfig().first()
        assertNotNull(result)
        assertEquals(ThemeMode.SYSTEM, result?.themeMode)
        assertEquals(PageDirection.RIGHT_TO_LEFT, result?.pageDirection)
        assertEquals(DoublePageMode.AUTO, result?.doublePageMode)
        assertEquals(true, result?.crossChapter)
        assertEquals(500, result?.cacheLimitMB)
        assertEquals(4, result?.thumbnailConcurrency)
        assertEquals(null, result?.autoSyncInterval)
    }
}
