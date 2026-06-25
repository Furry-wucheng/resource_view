package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.entity.SourceType
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ResourceTagDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var resourceTagDao: ResourceTagDao
    private lateinit var resourceDao: ResourceDao
    private lateinit var sourceDao: SourceDao
    private lateinit var tagDao: TagDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        resourceTagDao = db.resourceTagDao()
        resourceDao = db.resourceDao()
        sourceDao = db.sourceDao()
        tagDao = db.tagDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createTestSource() = SourceEntity(
        id = UUID.randomUUID().toString(),
        name = "Test Source",
        type = SourceType.LOCAL,
        rootPath = "/test",
    )

    private fun createTestResource(sourceId: String) = ResourceEntity(
        id = UUID.randomUUID().toString(),
        sourceId = sourceId,
        name = "Test Resource",
        type = ResourceType.FOLDER,
        relativePath = "/test/resource",
    )

    private fun createTestTag(name: String = "Test Tag") = TagEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        color = "#FF0000",
    )

    @Test
    fun `should insert resource tag`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource = createTestResource(source.id)
        resourceDao.insert(resource)
        val tag = createTestTag()
        tagDao.insert(tag)

        val resourceTag = ResourceTagEntity(resourceId = resource.id, tagId = tag.id)
        resourceTagDao.insert(resourceTag)

        val result = resourceTagDao.getByResourceId(resource.id)
        assertEquals(1, result.size)
        assertEquals(tag.id, result[0].tagId)
    }

    @Test
    fun `should insert all resource tags`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource = createTestResource(source.id)
        resourceDao.insert(resource)
        val tag1 = createTestTag(name = "Tag 1")
        val tag2 = createTestTag(name = "Tag 2")
        tagDao.insert(tag1)
        tagDao.insert(tag2)

        val resourceTags = listOf(
            ResourceTagEntity(resourceId = resource.id, tagId = tag1.id),
            ResourceTagEntity(resourceId = resource.id, tagId = tag2.id),
        )
        resourceTagDao.insertAll(resourceTags)

        val result = resourceTagDao.getByResourceId(resource.id)
        assertEquals(2, result.size)
    }

    @Test
    fun `should delete resource tag`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource = createTestResource(source.id)
        resourceDao.insert(resource)
        val tag = createTestTag()
        tagDao.insert(tag)

        val resourceTag = ResourceTagEntity(resourceId = resource.id, tagId = tag.id)
        resourceTagDao.insert(resourceTag)
        resourceTagDao.delete(resourceTag)

        val result = resourceTagDao.getByResourceId(resource.id)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should delete by resource and tag`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource = createTestResource(source.id)
        resourceDao.insert(resource)
        val tag = createTestTag()
        tagDao.insert(tag)

        resourceTagDao.insert(ResourceTagEntity(resourceId = resource.id, tagId = tag.id))
        resourceTagDao.deleteByResourceAndTag(resource.id, tag.id)

        val result = resourceTagDao.getByResourceId(resource.id)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should delete by resource id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource = createTestResource(source.id)
        resourceDao.insert(resource)
        val tag1 = createTestTag(name = "Tag 1")
        val tag2 = createTestTag(name = "Tag 2")
        tagDao.insert(tag1)
        tagDao.insert(tag2)

        resourceTagDao.insert(ResourceTagEntity(resourceId = resource.id, tagId = tag1.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource.id, tagId = tag2.id))
        resourceTagDao.deleteByResourceId(resource.id)

        val result = resourceTagDao.getByResourceId(resource.id)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should delete by tag id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource1 = createTestResource(source.id)
        val resource2 = createTestResource(source.id)
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)
        val tag = createTestTag()
        tagDao.insert(tag)

        resourceTagDao.insert(ResourceTagEntity(resourceId = resource1.id, tagId = tag.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource2.id, tagId = tag.id))
        resourceTagDao.deleteByTagId(tag.id)

        val result1 = resourceTagDao.getByResourceId(resource1.id)
        val result2 = resourceTagDao.getByResourceId(resource2.id)
        assertTrue(result1.isEmpty())
        assertTrue(result2.isEmpty())
    }

    @Test
    fun `should count by tag id`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource1 = createTestResource(source.id)
        val resource2 = createTestResource(source.id)
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)
        val tag = createTestTag()
        tagDao.insert(tag)

        resourceTagDao.insert(ResourceTagEntity(resourceId = resource1.id, tagId = tag.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource2.id, tagId = tag.id))

        val count = resourceTagDao.countByTagId(tag.id)
        assertEquals(2, count)
    }

    @Test
    fun `should ignore duplicate insert`() = runTest {
        val source = createTestSource()
        sourceDao.insert(source)
        val resource = createTestResource(source.id)
        resourceDao.insert(resource)
        val tag = createTestTag()
        tagDao.insert(tag)

        val resourceTag = ResourceTagEntity(resourceId = resource.id, tagId = tag.id)
        resourceTagDao.insert(resourceTag)
        resourceTagDao.insert(resourceTag)

        val result = resourceTagDao.getByResourceId(resource.id)
        assertEquals(1, result.size)
    }
}
