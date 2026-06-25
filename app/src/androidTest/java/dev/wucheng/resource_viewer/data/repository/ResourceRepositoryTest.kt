package dev.wucheng.resource_viewer.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity
import dev.wucheng.resource_viewer.domain.error.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
 * ResourceRepository 测试。
 * 使用 Room 内存数据库进行集成测试。
 */
@RunWith(AndroidJUnit4::class)
class ResourceRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ResourceRepository
    private lateinit var testSourceId: String

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = ResourceRepository(db.resourceDao(), db.tagDao(), db.resourceTagDao())
        testSourceId = UUID.randomUUID().toString()
        // Insert test source
        runBlocking {
            db.sourceDao().insert(SourceEntity(
                id = testSourceId,
                name = "Test Source",
                type = SourceType.LOCAL,
                rootPath = "/test",
            ))
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `should return Ok when resource inserted successfully`() = runTest {
        val resource = createTestResource()
        val result = repo.insert(resource)
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should return resource by id when exists`() = runTest {
        val resource = createTestResource()
        repo.insert(resource)
        val result = repo.getById(resource.id)
        assertTrue(result is Result.Ok)
        assertEquals(resource.id, (result as Result.Ok).value?.id)
    }

    @Test
    fun `should return null when resource not found`() = runTest {
        val result = repo.getById("non-existent-id")
        assertTrue(result is Result.Ok)
        assertNull((result as Result.Ok).value)
    }

    @Test
    fun `should return visible resources`() = runTest {
        val resource1 = createTestResource(name = "Resource 1")
        val resource2 = createTestResource(name = "Resource 2")
        repo.insert(resource1)
        repo.insert(resource2)
        val resources = repo.getVisibleResources().first()
        assertEquals(2, resources.size)
    }

    @Test
    fun `should return available resources only`() = runTest {
        val resource1 = createTestResource(name = "Available", isAvailable = true)
        val resource2 = createTestResource(name = "Unavailable", isAvailable = false)
        repo.insert(resource1)
        repo.insert(resource2)
        val resources = repo.getAvailableResources().first()
        assertEquals(1, resources.size)
        assertEquals("Available", resources[0].name)
    }

    @Test
    fun `should batch insert resources`() = runTest {
        val resources = listOf(
            createTestResource(name = "Resource 1"),
            createTestResource(name = "Resource 2"),
            createTestResource(name = "Resource 3"),
        )
        val result = repo.insertAll(resources)
        assertTrue(result is Result.Ok)
        val allResources = repo.getVisibleResources().first()
        assertEquals(3, allResources.size)
    }

    @Test
    fun `should update resource successfully`() = runTest {
        val resource = createTestResource()
        repo.insert(resource)
        val updated = resource.copy(name = "Updated Name")
        val result = repo.update(updated)
        assertTrue(result is Result.Ok)
        val fetched = repo.getById(resource.id)
        assertEquals("Updated Name", (fetched as Result.Ok).value?.name)
    }

    @Test
    fun `should delete resource successfully`() = runTest {
        val resource = createTestResource()
        repo.insert(resource)
        val result = repo.deleteById(resource.id)
        assertTrue(result is Result.Ok)
        val fetched = repo.getById(resource.id)
        assertNull((fetched as Result.Ok).value)
    }

    @Test
    fun `should delete resources by source id`() = runTest {
        val resource1 = createTestResource(name = "Resource 1")
        val resource2 = createTestResource(name = "Resource 2")
        repo.insert(resource1)
        repo.insert(resource2)
        val result = repo.deleteBySourceId(testSourceId)
        assertTrue(result is Result.Ok)
        val resources = repo.getVisibleResources().first()
        assertEquals(0, resources.size)
    }

    @Test
    fun `should page resources correctly`() = runTest {
        val resource1 = createTestResource(name = "Resource 1")
        val resource2 = createTestResource(name = "Resource 2")
        val resource3 = createTestResource(name = "Resource 3")
        repo.insert(resource1)
        repo.insert(resource2)
        repo.insert(resource3)
        val result = repo.pageAfter(System.currentTimeMillis() + 1000, 2)
        assertTrue(result is Result.Ok)
        assertEquals(2, (result as Result.Ok).value.size)
    }

    @Test
    fun `should search resources by name`() = runTest {
        val resource1 = createTestResource(name = "Comic Book")
        val resource2 = createTestResource(name = "Movie")
        val resource3 = createTestResource(name = "Comic Strip")
        repo.insert(resource1)
        repo.insert(resource2)
        repo.insert(resource3)
        val resources = repo.searchByName("Comic").first()
        assertEquals(2, resources.size)
    }

    @Test
    fun `should filter resources by tags intersection`() = runTest {
        val resource1 = createTestResource(name = "Resource 1")
        val resource2 = createTestResource(name = "Resource 2")
        repo.insert(resource1)
        repo.insert(resource2)

        val tag1 = TagEntity(id = "tag1", name = "Tag 1", color = "#FF0000")
        val tag2 = TagEntity(id = "tag2", name = "Tag 2", color = "#00FF00")
        db.tagDao().insert(tag1)
        db.tagDao().insert(tag2)

        // resource1 has both tags
        db.resourceTagDao().insert(ResourceTagEntity(resourceId = resource1.id, tagId = "tag1"))
        db.resourceTagDao().insert(ResourceTagEntity(resourceId = resource1.id, tagId = "tag2"))
        // resource2 has only tag1
        db.resourceTagDao().insert(ResourceTagEntity(resourceId = resource2.id, tagId = "tag1"))

        val resources = repo.filterByTags(listOf("tag1", "tag2")).first()
        assertEquals(1, resources.size)
        assertEquals("Resource 1", resources[0].name)
    }

    private fun createTestResource(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Resource",
        type: ResourceType = ResourceType.FOLDER,
        isAvailable: Boolean = true,
    ) = ResourceEntity(
        id = id,
        sourceId = testSourceId,
        name = name,
        type = type,
        relativePath = "/test/$name",
        isAvailable = isAvailable,
    )
}
