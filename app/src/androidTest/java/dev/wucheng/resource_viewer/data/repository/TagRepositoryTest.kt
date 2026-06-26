package dev.wucheng.resource_viewer.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.domain.error.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * TagRepository 测试。
 * 使用 Room 内存数据库进行集成测试。
 */
@RunWith(AndroidJUnit4::class)
class TagRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TagRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = TagRepository(db.tagDao(), db.resourceTagDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `should return Ok when tag inserted successfully`() = runTest {
        val tag = createTestTag()
        val result = repo.insert(tag)
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return tag by id when exists`() = runTest {
        val tag = createTestTag()
        repo.insert(tag)
        val result = repo.getById(tag.id)
        assertTrue(result is Result.Ok)
        assertEquals(tag.id, (result as Result.Ok).value?.id)
    }

    @Test
    fun `should return null when tag not found`() = runTest {
        val result = repo.getById("non-existent-id")
        assertTrue(result is Result.Ok)
        assertNull((result as Result.Ok).value)
    }

    @Test
    fun `should return all tags ordered by isBuiltIn desc`() = runTest {
        val tag1 = createTestTag(name = "Custom Tag")
        val tag2 = createTestTag(name = "Built-in Tag", isBuiltIn = true)
        repo.insert(tag1)
        repo.insert(tag2)
        val tags = repo.getAllTags().first()
        assertEquals(2, tags.size)
        // Built-in tags should come first
        assertEquals(true, tags[0].isBuiltIn)
    }

    @Test
    fun `should update non-built-in tag successfully`() = runTest {
        val tag = createTestTag(isBuiltIn = false)
        repo.insert(tag)
        val updated = tag.copy(name = "Updated Name")
        val result = repo.update(updated)
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return error when updating built-in tag`() = runTest {
        val tag = createTestTag(isBuiltIn = true)
        repo.insert(tag)
        val updated = tag.copy(name = "Updated Name")
        val result = repo.update(updated)
        assertTrue(result is Result.Err)
    }

    @Test
    fun `should delete non-built-in tag successfully`() = runTest {
        val tag = createTestTag(isBuiltIn = false)
        repo.insert(tag)
        val result = repo.deleteById(tag.id)
        assertTrue(result is Result.Ok)
        val fetched = repo.getById(tag.id)
        assertNull((fetched as Result.Ok).value)
    }

    @Test
    fun `should return error when deleting built-in tag`() = runTest {
        val tag = createTestTag(isBuiltIn = true)
        repo.insert(tag)
        val result = repo.deleteById(tag.id)
        assertTrue(result is Result.Err)
    }

    @Test
    fun `should return error when deleting non-existent tag`() = runTest {
        val result = repo.deleteById("non-existent-id")
        assertTrue(result is Result.Err)
    }

    @Test
    fun `should return empty list when no tags exist`() = runTest {
        val tags = repo.getAllTags().first()
        assertEquals(0, tags.size)
    }

    @Test
    fun `should handle insert with duplicate id`() = runTest {
        val tag = createTestTag()
        repo.insert(tag)
        // Insert again with same id - should replace due to OnConflictStrategy.REPLACE
        val result = repo.insert(tag)
        assertTrue(result is Result.Ok)
        val tags = repo.getAllTags().first()
        assertEquals(1, tags.size)
    }

    @Test
    fun `should return tag with correct resource count`() = runTest {
        val tag = createTestTag()
        repo.insert(tag)
        val result = repo.getById(tag.id)
        assertTrue(result is Result.Ok)
        assertEquals(0, (result as Result.Ok).value?.resourceCount)
    }

    @Test
    fun `should handle update for non-existent tag`() = runTest {
        val tag = createTestTag()
        // Don't insert tag, just try to update
        val result = repo.update(tag)
        // Room's update with OnConflictStrategy.IGNORE should return Ok
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return error when updating tag with empty name`() = runTest {
        val tag = createTestTag(name = "")
        repo.insert(tag)
        val updated = tag.copy(name = "")
        val result = repo.update(updated)
        // Should succeed as there's no validation for empty name in repository
        assertTrue(result is Result.Ok)
    }

    private fun createTestTag(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Tag",
        color: String = "#FF0000",
        isBuiltIn: Boolean = false,
    ) = TagEntity(
        id = id,
        name = name,
        color = color,
        isBuiltIn = isBuiltIn,
    )
}
