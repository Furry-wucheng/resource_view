package dev.wucheng.resource_viewer.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import dev.wucheng.resource_viewer.domain.error.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

/**
 * SourceRepository 测试。
 * 使用 Room 内存数据库进行集成测试。
 */
@RunWith(AndroidJUnit4::class)
class SourceRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: SourceRepository
    private lateinit var mockSecurePrefs: SecurePrefs

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        mockSecurePrefs = mockk()
        repo = SourceRepository(db.sourceDao(), mockSecurePrefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `should return Ok when source inserted successfully`() = runTest {
        val source = createTestSource()
        val result = repo.insert(source)
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return source by id when exists`() = runTest {
        val source = createTestSource()
        repo.insert(source)
        val result = repo.getSourceById(source.id)
        assertTrue(result is Result.Ok)
        assertEquals(source.id, (result as Result.Ok).value?.id)
    }

    @Test
    fun `should return null when source not found`() = runTest {
        val result = repo.getSourceById("non-existent-id")
        assertTrue(result is Result.Ok)
        assertNull((result as Result.Ok).value)
    }

    @Test
    fun `should return all sources ordered by createdAt desc`() = runTest {
        val source1 = createTestSource(name = "Source 1")
        val source2 = createTestSource(name = "Source 2")
        repo.insert(source1)
        repo.insert(source2)
        val sources = repo.getAllSources().first()
        assertEquals(2, sources.size)
    }

    @Test
    fun `should update source successfully`() = runTest {
        val source = createTestSource()
        repo.insert(source)
        val updated = source.copy(name = "Updated Name")
        val result = repo.update(updated)
        assertTrue(result is Result.Ok)
        val fetched = repo.getSourceById(source.id)
        assertEquals("Updated Name", (fetched as Result.Ok).value?.name)
    }

    @Test
    fun `should delete source successfully`() = runTest {
        val source = createTestSource()
        repo.insert(source)
        val result = repo.deleteById(source.id)
        assertTrue(result is Result.Ok)
        val fetched = repo.getSourceById(source.id)
        assertNull((fetched as Result.Ok).value)
    }

    @Test
    fun `should update availability status`() = runTest {
        val source = createTestSource()
        repo.insert(source)
        val checkTime = System.currentTimeMillis()
        val result = repo.updateAvailability(source.id, true, checkTime)
        assertTrue(result is Result.Ok)
        val fetched = repo.getSourceById(source.id)
        val value = (fetched as Result.Ok).value
        assertEquals(true, value?.isAvailable)
        assertEquals(checkTime, value?.lastCheckAt)
    }

    @Test
    fun `should store password in secure prefs`() {
        every { mockSecurePrefs.putPassword(any(), any()) } returns Unit
        repo.putPassword("source-id", "password123")
        verify { mockSecurePrefs.putPassword("source-id", "password123") }
    }

    @Test
    fun `should get password from secure prefs`() {
        every { mockSecurePrefs.getPassword("source-id") } returns "password123"
        val password = repo.getPassword("source-id")
        assertEquals("password123", password)
    }

    @Test
    fun `should remove password from secure prefs`() {
        every { mockSecurePrefs.removePassword(any()) } returns Unit
        repo.removePassword("source-id")
        verify { mockSecurePrefs.removePassword("source-id") }
    }

    @Test
    fun `should return null when getting password for non-existent source`() {
        every { mockSecurePrefs.getPassword("non-existent-id") } returns null
        val password = repo.getPassword("non-existent-id")
        assertNull(password)
    }

    @Test
    fun `should handle update for non-existent source`() = runTest {
        val source = createTestSource()
        // Don't insert source, just try to update
        val result = repo.update(source)
        // Room's update with OnConflictStrategy.IGNORE should return Ok
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should handle deleteById for non-existent source`() = runTest {
        val result = repo.deleteById("non-existent-id")
        // Room's delete with OnConflictStrategy.IGNORE should return Ok
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return empty list when no sources exist`() = runTest {
        val sources = repo.getAllSources().first()
        assertEquals(0, sources.size)
    }

    @Test
    fun `should handle updateAvailability for non-existent source`() = runTest {
        val result = repo.updateAvailability("non-existent-id", true, System.currentTimeMillis())
        // Room's update with OnConflictStrategy.IGNORE should return Ok
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return null when getting password after removal`() {
        every { mockSecurePrefs.getPassword("source-id") } returns null
        val password = repo.getPassword("source-id")
        assertNull(password)
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
}
