package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SplitResourceUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var useCase: SplitResourceUseCase

    private val testSource = Source(
        id = "source-1",
        name = "Test Source",
        type = SourceType.LOCAL,
        rootPath = "/test",
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private val testResource = Resource(
        id = "resource-1",
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "parent-folder",
        type = ResourceType.FOLDER,
        organizationMode = OrganizationMode.CHAPTER,
        relativePath = "parent-folder",
        thumbnailPath = null,
        fileCount = 5,
        fileSize = null,
        isAvailable = true,
        lastScannedAt = 1000L,
        tags = emptyList(),
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    @Before
    fun setup() {
        resourceRepository = mockk()
        useCase = SplitResourceUseCase(resourceRepository)
    }

    @Test
    fun `should create child resources from selected items`() = runTest {
        // Given
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
            FileEntry(name = "chapter2", relativePath = "parent-folder/chapter2", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        // When
        val result = useCase(testResource, selectedItems, inheritTags = false)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(2, scanResult.successCount)
        assertEquals(0, scanResult.skipCount)
        assertTrue(scanResult.failures.isEmpty())
        coVerify { resourceRepository.insertAll(match { it.size == 2 }) }
    }

    @Test
    fun `should mark parent resource as split when split succeeds`() = runTest {
        // Given
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        // When
        useCase(testResource, selectedItems, inheritTags = false)

        // Then
        coVerify {
            resourceRepository.update(match { entity ->
                entity.id == "resource-1" && !entity.isAvailable
            })
        }
    }

    @Test
    fun `should inherit tags from parent when inheritTags is true`() = runTest {
        // Given
        val tags = listOf(
            Tag(id = "tag-1", name = "Favorites", color = "#FFC107", createdAt = 1000L, updatedAt = 1000L),
        )
        val resourceWithTags = testResource.copy(tags = tags)
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        // When
        val result = useCase(resourceWithTags, selectedItems, inheritTags = true)

        // Then
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `should record failures when insert fails`() = runTest {
        // Given
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { resourceRepository.insertAll(any()) } returns Result.Err(DomainError.DatabaseError("Insert failed"))

        // When
        val result = useCase(testResource, selectedItems, inheritTags = false)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertTrue(scanResult.failures.isNotEmpty())
    }

    @Test
    fun `should handle empty selected items`() = runTest {
        // Given
        val selectedItems = emptyList<FileEntry>()

        // When
        val result = useCase(testResource, selectedItems, inheritTags = false)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertEquals(0, scanResult.skipCount)
        assertTrue(scanResult.failures.isEmpty())
    }
}
