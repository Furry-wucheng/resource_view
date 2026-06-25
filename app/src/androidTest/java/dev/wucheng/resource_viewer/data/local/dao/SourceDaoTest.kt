package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SourceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var sourceDao: SourceDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sourceDao = db.sourceDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createTestSource(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Source",
        type: SourceType = SourceType.LOCAL,
        rootPath: String = "/test/path",
    ) = SourceEntity(
        id = id,
        name = name,
        type = type,
        rootPath = rootPath,
    )

    @Test
    fun `should insert and get source by id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val result = sourceDao.getSourceById(source.id)
        assertNotNull(result)
        assertEquals(source.id, result?.id)
        assertEquals(source.name, result?.name)
    }

    @Test
    fun `should return null when source not found`() = runTest {
        val result = sourceDao.getSourceById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun `should get all sources ordered by createdAt desc`() = runTest {
        val source1 = createTestSource(name = "First")
        val source2 = createTestSource(name = "Second")
        sourceDao.insert(source1)
        sourceDao.insert(source2)

        val sources = sourceDao.getAllSources().first()
        assertEquals(2, sources.size)
        assertEquals(source2.id, sources[0].id)
        assertEquals(source1.id, sources[1].id)
    }

    @Test
    fun `should update source`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val updated = source.copy(name = "Updated Name")
        sourceDao.update(updated)

        val result = sourceDao.getSourceById(source.id)
        assertEquals("Updated Name", result?.name)
    }

    @Test
    fun `should delete source by id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        sourceDao.deleteById(source.id)

        val result = sourceDao.getSourceById(source.id)
        assertNull(result)
    }

    @Test
    fun `should update availability`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val checkTime = System.currentTimeMillis()
        sourceDao.updateAvailability(source.id, true, checkTime)

        val result = sourceDao.getSourceById(source.id)
        assertEquals(true, result?.isAvailable)
        assertEquals(checkTime, result?.lastCheckAt)
    }

    @Test
    fun `should replace source on conflict`() = runTest {
        val id = UUID.randomUUID().toString()
        val source1 = createTestSource(id = id, name = "Original")
        val source2 = createTestSource(id = id, name = "Replaced")

        sourceDao.insert(source1)
        sourceDao.insert(source2)

        val sources = sourceDao.getAllSources().first()
        assertEquals(1, sources.size)
        assertEquals("Replaced", sources[0].name)
    }
}
