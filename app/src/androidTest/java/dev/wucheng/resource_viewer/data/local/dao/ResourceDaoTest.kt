package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
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
class ResourceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var resourceDao: ResourceDao
    private lateinit var sourceDao: SourceDao
    private lateinit var tagDao: TagDao
    private lateinit var resourceTagDao: ResourceTagDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        resourceDao = db.resourceDao()
        sourceDao = db.sourceDao()
        tagDao = db.tagDao()
        resourceTagDao = db.resourceTagDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createTestSource(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Source",
    ) = SourceEntity(
        id = id,
        name = name,
        type = SourceType.LOCAL,
        rootPath = "/test/path",
    )

    private fun createTestResource(
        id: String = UUID.randomUUID().toString(),
        sourceId: String,
        name: String = "Test Resource",
        type: ResourceType = ResourceType.FOLDER,
        isAvailable: Boolean = true,
        createdAt: Long = System.currentTimeMillis(),
    ) = ResourceEntity(
        id = id,
        sourceId = sourceId,
        name = name,
        type = type,
        relativePath = "/test/resource",
        isAvailable = isAvailable,
        createdAt = createdAt,
    )

    private fun createTestTag(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Tag",
    ) = TagEntity(
        id = id,
        name = name,
        color = "#FF0000",
    )

    @Test
    fun `should insert and get resource by id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource = createTestResource(sourceId = source.id)
        resourceDao.insert(resource)

        val result = resourceDao.getById(resource.id)
        assertNotNull(result)
        assertEquals(resource.id, result?.id)
        assertEquals(resource.name, result?.name)
    }

    @Test
    fun `should return null when resource not found`() = runTest {
        val result = resourceDao.getById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun `should get visible resources ordered by createdAt desc`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource1 = createTestResource(sourceId = source.id, name = "First", createdAt = 1000)
        val resource2 = createTestResource(sourceId = source.id, name = "Second", createdAt = 2000)
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)

        val resources = resourceDao.getVisibleResources().first()
        assertEquals(2, resources.size)
        assertEquals(resource2.id, resources[0].id)
        assertEquals(resource1.id, resources[1].id)
    }

    @Test
    fun `should get only available resources`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val available = createTestResource(sourceId = source.id, name = "Available", isAvailable = true)
        val unavailable = createTestResource(sourceId = source.id, name = "Unavailable", isAvailable = false)
        resourceDao.insert(available)
        resourceDao.insert(unavailable)

        val resources = resourceDao.getAvailableResources().first()
        assertEquals(1, resources.size)
        assertEquals("Available", resources[0].name)
    }

    @Test
    fun `should update resource`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource = createTestResource(sourceId = source.id)
        resourceDao.insert(resource)

        val updated = resource.copy(name = "Updated Name")
        resourceDao.update(updated)

        val result = resourceDao.getById(resource.id)
        assertEquals("Updated Name", result?.name)
    }

    @Test
    fun `should delete resource by id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource = createTestResource(sourceId = source.id)
        resourceDao.insert(resource)

        resourceDao.deleteById(resource.id)

        val result = resourceDao.getById(resource.id)
        assertNull(result)
    }

    @Test
    fun `should delete resources by source id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource1 = createTestResource(sourceId = source.id, name = "First")
        val resource2 = createTestResource(sourceId = source.id, name = "Second")
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)

        resourceDao.deleteBySourceId(source.id)

        val resources = resourceDao.getVisibleResources().first()
        assertTrue(resources.isEmpty())
    }

    @Test
    fun `should insert all resources with replace on conflict`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val id = UUID.randomUUID().toString()
        val resource1 = createTestResource(id = id, sourceId = source.id, name = "Original")
        val resource2 = createTestResource(id = id, sourceId = source.id, name = "Replaced")

        resourceDao.insertAll(listOf(resource1, resource2))

        val resources = resourceDao.getVisibleResources().first()
        assertEquals(1, resources.size)
        assertEquals("Replaced", resources[0].name)
    }

    @Test
    fun `should ignore duplicate insert`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource = createTestResource(sourceId = source.id, name = "Original")
        resourceDao.insert(resource)

        val duplicate = resource.copy(name = "Duplicate")
        resourceDao.insert(duplicate)

        val resources = resourceDao.getVisibleResources().first()
        assertEquals(1, resources.size)
        assertEquals("Original", resources[0].name)
    }

    @Test
    fun `should page after cursor`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource1 = createTestResource(sourceId = source.id, name = "First", createdAt = 1000)
        val resource2 = createTestResource(sourceId = source.id, name = "Second", createdAt = 2000)
        val resource3 = createTestResource(sourceId = source.id, name = "Third", createdAt = 3000)
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)
        resourceDao.insert(resource3)

        val page = resourceDao.pageAfter(beforeCreatedAt = 3000, limit = 2)
        assertEquals(2, page.size)
        assertEquals("Second", page[0].name)
        assertEquals("First", page[1].name)
    }

    @Test
    fun `should filter by tags intersection`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource1 = createTestResource(sourceId = source.id, name = "Resource 1")
        val resource2 = createTestResource(sourceId = source.id, name = "Resource 2")
        val resource3 = createTestResource(sourceId = source.id, name = "Resource 3")
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)
        resourceDao.insert(resource3)

        val tag1 = createTestTag(name = "Tag 1")
        val tag2 = createTestTag(name = "Tag 2")
        tagDao.insert(tag1)
        tagDao.insert(tag2)

        resourceTagDao.insert(ResourceTagEntity(resourceId = resource1.id, tagId = tag1.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource1.id, tagId = tag2.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource2.id, tagId = tag1.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource3.id, tagId = tag2.id))

        val filtered = resourceDao.filterByTags(listOf(tag1.id, tag2.id), tagCount = 2).first()
        assertEquals(1, filtered.size)
        assertEquals(resource1.id, filtered[0].id)
    }

    @Test
    fun `should search resources by name`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)

        val resource1 = createTestResource(sourceId = source.id, name = "Comic Book")
        val resource2 = createTestResource(sourceId = source.id, name = "Movie")
        val resource3 = createTestResource(sourceId = source.id, name = "Comic Movie")
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)
        resourceDao.insert(resource3)

        val results = resourceDao.searchByName("Comic").first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.name.contains("Comic") })
    }
}
