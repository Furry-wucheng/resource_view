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
class TagDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var tagDao: TagDao
    private lateinit var resourceDao: ResourceDao
    private lateinit var sourceDao: SourceDao
    private lateinit var resourceTagDao: ResourceTagDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tagDao = db.tagDao()
        resourceDao = db.resourceDao()
        sourceDao = db.sourceDao()
        resourceTagDao = db.resourceTagDao()
    }

    @After
    fun teardown() {
        db.close()
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

    @Test
    fun `should insert and get tag by id`() = runTest {
        val tag = createTestTag()
        tagDao.insert(tag)

        val result = tagDao.getById(tag.id)
        assertNotNull(result)
        assertEquals(tag.id, result?.id)
        assertEquals(tag.name, result?.name)
    }

    @Test
    fun `should return null when tag not found`() = runTest {
        val result = tagDao.getById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun `should get all tags ordered by isBuiltIn desc then createdAt desc`() = runTest {
        val customTag = createTestTag(name = "Custom", isBuiltIn = false)
        val builtInTag = createTestTag(name = "BuiltIn", isBuiltIn = true)
        tagDao.insert(customTag)
        tagDao.insert(builtInTag)

        val tags = tagDao.getAllTags().first()
        assertEquals(2, tags.size)
        assertEquals(builtInTag.id, tags[0].id)
        assertEquals(customTag.id, tags[1].id)
    }

    @Test
    fun `should get built in tags only`() = runTest {
        val customTag = createTestTag(name = "Custom", isBuiltIn = false)
        val builtInTag = createTestTag(name = "BuiltIn", isBuiltIn = true)
        tagDao.insert(customTag)
        tagDao.insert(builtInTag)

        val tags = tagDao.getBuiltInTags()
        assertEquals(1, tags.size)
        assertEquals(builtInTag.id, tags[0].id)
    }

    @Test
    fun `should update tag`() = runTest {
        val tag = createTestTag()
        tagDao.insert(tag)

        val updated = tag.copy(name = "Updated Name")
        tagDao.update(updated)

        val result = tagDao.getById(tag.id)
        assertEquals("Updated Name", result?.name)
    }

    @Test
    fun `should delete non built in tag`() = runTest {
        val tag = createTestTag(isBuiltIn = false)
        tagDao.insert(tag)

        tagDao.deleteById(tag.id)

        val result = tagDao.getById(tag.id)
        assertNull(result)
    }

    @Test
    fun `should not delete built in tag`() = runTest {
        val tag = createTestTag(isBuiltIn = true)
        tagDao.insert(tag)

        tagDao.deleteById(tag.id)

        val result = tagDao.getById(tag.id)
        assertNotNull(result)
    }

    @Test
    fun `should ignore duplicate tag insert`() = runTest {
        val id = UUID.randomUUID().toString()
        val tag1 = createTestTag(id = id, name = "Original")
        val tag2 = createTestTag(id = id, name = "Duplicate")

        tagDao.insert(tag1)
        tagDao.insert(tag2)

        val tags = tagDao.getAllTags().first()
        assertEquals(1, tags.size)
        assertEquals("Original", tags[0].name)
    }

    @Test
    fun `should get tag resource counts`() = runTest {
        val source = SourceEntity(
            id = UUID.randomUUID().toString(),
            name = "Test Source",
            type = SourceType.LOCAL,
            rootPath = "/test",
        )
        sourceDao.insert(source)

        val tag1 = createTestTag(name = "Tag 1")
        val tag2 = createTestTag(name = "Tag 2")
        tagDao.insert(tag1)
        tagDao.insert(tag2)

        val resource1 = ResourceEntity(
            id = UUID.randomUUID().toString(),
            sourceId = source.id,
            name = "Resource 1",
            type = ResourceType.FOLDER,
            relativePath = "/r1",
        )
        val resource2 = ResourceEntity(
            id = UUID.randomUUID().toString(),
            sourceId = source.id,
            name = "Resource 2",
            type = ResourceType.FOLDER,
            relativePath = "/r2",
        )
        resourceDao.insert(resource1)
        resourceDao.insert(resource2)

        resourceTagDao.insert(ResourceTagEntity(resourceId = resource1.id, tagId = tag1.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource2.id, tagId = tag1.id))
        resourceTagDao.insert(ResourceTagEntity(resourceId = resource1.id, tagId = tag2.id))

        val counts = tagDao.getTagResourceCounts().first()
        assertEquals(2, counts.size)

        val tag1Count = counts.find { it.id == tag1.id }
        val tag2Count = counts.find { it.id == tag2.id }

        assertEquals(2, tag1Count?.count)
        assertEquals(1, tag2Count?.count)
    }
}
