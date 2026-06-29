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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SplitResourceUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var detectOrgModeUseCase: DetectOrganizationModeUseCase
    private lateinit var fileSource: FileSource
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
        detectOrgModeUseCase = mockk()
        fileSource = mockk()
        every { resourceRepository.getVisibleResources() } returns flowOf(emptyList())
        useCase = SplitResourceUseCase(resourceRepository, detectOrgModeUseCase)
    }

    @Test
    fun `should create child resources with auto-detected organization mode`() = runTest {
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
            FileEntry(name = "chapter2", relativePath = "parent-folder/chapter2", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter1") } returns OrganizationMode.CHAPTER
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter2") } returns OrganizationMode.FLATGRID
        coEvery { resourceRepository.insertAll(match { it.size == 2 }) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        val result = useCase(testResource, selectedItems, fileSource, deleteOriginal = false)

        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(2, scanResult.successCount)
        assertEquals(0, scanResult.skipCount)
        assertEquals(0, scanResult.failures.size)
        coVerify { resourceRepository.insertAll(match { entities ->
            entities.size == 2 &&
            entities.any { it.name == "chapter1" && it.organizationMode == OrganizationMode.CHAPTER } &&
            entities.any { it.name == "chapter2" && it.organizationMode == OrganizationMode.FLATGRID }
        }) }
    }

    @Test
    fun `should mark parent as unavailable when keepOriginal`() = runTest {
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter1") } returns OrganizationMode.CHAPTER
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        useCase(testResource, selectedItems, fileSource, deleteOriginal = false)

        coVerify {
            resourceRepository.update(match { entity ->
                entity.id == "resource-1" && !entity.isAvailable
            })
        }
        coVerify(exactly = 0) { resourceRepository.deleteById(any()) }
    }

    @Test
    fun `should delete parent resource when deleteOriginal is true`() = runTest {
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter1") } returns OrganizationMode.CHAPTER
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.deleteById("resource-1") } returns Result.Ok(Unit)

        useCase(testResource, selectedItems, fileSource, deleteOriginal = true)

        coVerify { resourceRepository.deleteById("resource-1") }
        coVerify(exactly = 0) { resourceRepository.update(any()) }
    }

    @Test
    fun `should inherit tags from parent when inheritTags is true`() = runTest {
        val tags = listOf(
            Tag(id = "tag-1", name = "Favorites", color = "#FFC107", createdAt = 1000L, updatedAt = 1000L),
        )
        val resourceWithTags = testResource.copy(tags = tags)
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter1") } returns OrganizationMode.CHAPTER
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.setResourceTags(any(), any()) } returns Result.Ok(Unit)

        useCase(resourceWithTags, selectedItems, fileSource, deleteOriginal = false, inheritTags = true)

        coVerify { resourceRepository.setResourceTags(match { it.isNotEmpty() }, listOf("tag-1")) }
    }

    @Test
    fun `should not inherit tags when inheritTags is false`() = runTest {
        val tags = listOf(
            Tag(id = "tag-1", name = "Favorites", color = "#FFC107", createdAt = 1000L, updatedAt = 1000L),
        )
        val resourceWithTags = testResource.copy(tags = tags)
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter1") } returns OrganizationMode.CHAPTER
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        useCase(resourceWithTags, selectedItems, fileSource, deleteOriginal = false, inheritTags = false)

        coVerify(exactly = 0) { resourceRepository.setResourceTags(any(), any()) }
    }

    @Test
    fun `should skip items already existing in database`() = runTest {
        val existingResource = testResource.copy(
            id = "existing-1",
            relativePath = "parent-folder/chapter1",
        )
        every { resourceRepository.getVisibleResources() } returns flowOf(listOf(existingResource))

        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
            FileEntry(name = "chapter2", relativePath = "parent-folder/chapter2", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter2") } returns OrganizationMode.FLATGRID
        coEvery { resourceRepository.insertAll(match { it.size == 1 }) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        val result = useCase(testResource, selectedItems, fileSource, deleteOriginal = false)

        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(1, scanResult.successCount)
        assertEquals(1, scanResult.skipCount)
    }

    @Test
    fun `should deduplicate selected items`() = runTest {
        val selectedItems = listOf(
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
            FileEntry(name = "chapter1", relativePath = "parent-folder/chapter1", isDirectory = true, size = 0, modifiedAt = 1000L),
        )
        coEvery { detectOrgModeUseCase(fileSource, "parent-folder/chapter1") } returns OrganizationMode.CHAPTER
        coEvery { resourceRepository.insertAll(match { it.size == 1 }) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        val result = useCase(testResource, selectedItems, fileSource, deleteOriginal = false)

        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(1, scanResult.successCount)
        assertEquals(1, scanResult.skipCount)
    }

    @Test
    fun `should not delete parent when no children inserted`() = runTest {
        val selectedItems = listOf(
            FileEntry(name = "badfile", relativePath = "parent-folder/badfile", isDirectory = false, size = 0, modifiedAt = 1000L, extension = "exe"),
        )
        val result = useCase(testResource, selectedItems, fileSource, deleteOriginal = true)

        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertEquals(1, scanResult.skipCount)
        coVerify(exactly = 0) { resourceRepository.deleteById(any()) }
    }

    @Test
    fun `should handle empty selected items`() = runTest {
        val selectedItems = emptyList<FileEntry>()

        val result = useCase(testResource, selectedItems, fileSource, deleteOriginal = false)

        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertEquals(0, scanResult.skipCount)
        assertEquals(0, scanResult.failures.size)
    }

    @Test
    fun `should set orgMode to null for non-folder types`() = runTest {
        val selectedItems = listOf(
            FileEntry(name = "doc.pdf", relativePath = "parent-folder/doc.pdf", isDirectory = false, size = 1024, modifiedAt = 1000L, extension = "pdf"),
        )
        coEvery { resourceRepository.insertAll(match { it.size == 1 }) } returns Result.Ok(Unit)
        coEvery { resourceRepository.update(any()) } returns Result.Ok(Unit)

        val result = useCase(testResource, selectedItems, fileSource, deleteOriginal = false)

        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(1, scanResult.successCount)
        coVerify { resourceRepository.insertAll(match { entities ->
            entities.size == 1 && entities.all { it.organizationMode == null }
        }) }
        coVerify(exactly = 0) { detectOrgModeUseCase(any(), any()) }
    }
}
