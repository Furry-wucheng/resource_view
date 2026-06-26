package dev.wucheng.resource_viewer.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailGenerator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * ThumbnailRepository 测试。
 * 使用 MockK 进行单元测试。
 */
@RunWith(AndroidJUnit4::class)
class ThumbnailRepositoryTest {
    private lateinit var repo: ThumbnailRepository
    private lateinit var mockGenerator: ThumbnailGenerator
    private lateinit var mockFileSource: FileSource

    @Before
    fun setup() {
        mockGenerator = mockk()
        mockFileSource = mockk()
        repo = ThumbnailRepository(setOf(mockGenerator))
    }

    @Test
    fun `should return null when no generator can handle resource type`() = runTest {
        every { mockGenerator.canHandle(ResourceType.PDF) } returns false
        val resource = createTestResource(type = ResourceType.PDF)
        val cacheDir = File.createTempFile("test", null).parentFile
        val result = repo.generateThumbnail(resource, mockFileSource, cacheDir)
        assertTrue(result is Result.Ok)
        assertNull((result as Result.Ok).value)
    }

    @Test
    fun `should return file when generator succeeds`() = runTest {
        val expectedFile = File.createTempFile("thumbnail", ".jpg")
        every { mockGenerator.canHandle(ResourceType.FOLDER) } returns true
        coEvery { mockGenerator.generate(any(), any(), any()) } returns expectedFile
        val resource = createTestResource(type = ResourceType.FOLDER)
        val cacheDir = File.createTempFile("test", null).parentFile
        val result = repo.generateThumbnail(resource, mockFileSource, cacheDir)
        assertTrue(result is Result.Ok)
        assertEquals(expectedFile, (result as Result.Ok).value)
        expectedFile.delete()
    }

    @Test
    fun `should return error when generator throws exception`() = runTest {
        every { mockGenerator.canHandle(ResourceType.FOLDER) } returns true
        coEvery { mockGenerator.generate(any(), any(), any()) } throws RuntimeException("Generation failed")
        val resource = createTestResource(type = ResourceType.FOLDER)
        val cacheDir = File.createTempFile("test", null).parentFile
        val result = repo.generateThumbnail(resource, mockFileSource, cacheDir)
        assertTrue(result is Result.Err)
    }

    @Test
    fun `should return true when generator exists for resource type`() {
        every { mockGenerator.canHandle(ResourceType.FOLDER) } returns true
        assertTrue(repo.hasGenerator(ResourceType.FOLDER))
    }

    @Test
    fun `should return false when no generator for resource type`() {
        every { mockGenerator.canHandle(ResourceType.PDF) } returns false
        assertFalse(repo.hasGenerator(ResourceType.PDF))
    }

    @Test
    fun `should return null when generator returns null`() = runTest {
        every { mockGenerator.canHandle(ResourceType.FOLDER) } returns true
        coEvery { mockGenerator.generate(any(), any(), any()) } returns null
        val resource = createTestResource(type = ResourceType.FOLDER)
        val cacheDir = File.createTempFile("test", null).parentFile
        val result = repo.generateThumbnail(resource, mockFileSource, cacheDir)
        assertTrue(result is Result.Ok)
        assertNull((result as Result.Ok).value)
    }

    @Test
    fun `should handle multiple generators and select correct one`() = runTest {
        val mockGenerator2 = mockk<ThumbnailGenerator>()
        val repoWithMultiple = ThumbnailRepository(setOf(mockGenerator, mockGenerator2))

        every { mockGenerator.canHandle(ResourceType.PDF) } returns false
        every { mockGenerator2.canHandle(ResourceType.PDF) } returns true
        coEvery { mockGenerator2.generate(any(), any(), any()) } returns File("test.pdf")

        val resource = createTestResource(type = ResourceType.PDF)
        val cacheDir = File.createTempFile("test", null).parentFile
        val result = repoWithMultiple.generateThumbnail(resource, mockFileSource, cacheDir)
        assertTrue(result is Result.Ok)
        assertNotNull((result as Result.Ok).value)
    }

    @Test
    fun `should return false when no generators registered`() {
        val repoWithNoGenerators = ThumbnailRepository(emptySet())
        assertFalse(repoWithNoGenerators.hasGenerator(ResourceType.FOLDER))
    }

    @Test
    fun `should return null when no generators registered and generate called`() = runTest {
        val repoWithNoGenerators = ThumbnailRepository(emptySet())
        val resource = createTestResource(type = ResourceType.FOLDER)
        val cacheDir = File.createTempFile("test", null).parentFile
        val result = repoWithNoGenerators.generateThumbnail(resource, mockFileSource, cacheDir)
        assertTrue(result is Result.Ok)
        assertNull((result as Result.Ok).value)
    }

    private fun createTestResource(
        type: ResourceType = ResourceType.FOLDER,
    ) = Resource(
        id = "test-id",
        sourceId = "source-id",
        sourceName = "Test Source",
        name = "Test Resource",
        type = type,
        organizationMode = null,
        relativePath = "/test",
        thumbnailPath = null,
        fileCount = null,
        fileSize = null,
        isAvailable = true,
        lastScannedAt = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )
}
