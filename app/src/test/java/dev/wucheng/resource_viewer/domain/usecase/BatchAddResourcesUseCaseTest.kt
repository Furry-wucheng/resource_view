package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BatchAddResourcesUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var detectOrganizationModeUseCase: DetectOrganizationModeUseCase
    private lateinit var useCase: BatchAddResourcesUseCase

    private val testSource = Source(
        id = "source-1",
        name = "Test Source",
        type = SourceType.LOCAL,
        rootPath = "/test",
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    @Before
    fun setup() {
        resourceRepository = mockk()
        detectOrganizationModeUseCase = mockk()
        useCase = BatchAddResourcesUseCase(resourceRepository, detectOrganizationModeUseCase)
    }

    @Test
    fun `should create resources for each valid path and insert all`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val paths = listOf("folder1", "document.pdf")
        val fileEntries = mapOf(
            "folder1" to FileEntry(name = "folder1", relativePath = "folder1", isDirectory = true, size = 0, modifiedAt = 1000L),
            "document.pdf" to FileEntry(name = "document.pdf", relativePath = "document.pdf", isDirectory = false, size = 2048, modifiedAt = 1000L, extension = "pdf"),
        )
        coEvery { fileSource.stat("folder1") } returns fileEntries["folder1"]
        coEvery { fileSource.stat("document.pdf") } returns fileEntries["document.pdf"]
        coEvery { detectOrganizationModeUseCase(fileSource, "folder1") } returns dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.FLATGRID
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)

        // When
        val result = useCase(fileSource, testSource, paths)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(2, scanResult.successCount)
        assertEquals(0, scanResult.skipCount)
        assertTrue(scanResult.failures.isEmpty())
        coVerify { resourceRepository.insertAll(match { it.size == 2 }) }
    }

    @Test
    fun `should skip paths that do not exist in file source`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val paths = listOf("nonexistent")
        coEvery { fileSource.stat("nonexistent") } returns null

        // When
        val result = useCase(fileSource, testSource, paths)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertEquals(1, scanResult.skipCount)
    }

    @Test
    fun `should record failures when insert fails`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val paths = listOf("folder1")
        coEvery { fileSource.stat("folder1") } returns FileEntry(
            name = "folder1", relativePath = "folder1", isDirectory = true, size = 0, modifiedAt = 1000L
        )
        coEvery { detectOrganizationModeUseCase(fileSource, "folder1") } returns dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.FLATGRID
        coEvery { resourceRepository.insertAll(any()) } returns Result.Err(DomainError.DatabaseError("Insert failed"))

        // When
        val result = useCase(fileSource, testSource, paths)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertTrue(scanResult.failures.isNotEmpty())
    }

    @Test
    fun `should handle empty paths list`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()

        // When
        val result = useCase(fileSource, testSource, emptyList())

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertEquals(0, scanResult.skipCount)
        assertTrue(scanResult.failures.isEmpty())
    }

    @Test
    fun `should skip unsupported file types`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val paths = listOf("readme.txt")
        coEvery { fileSource.stat("readme.txt") } returns FileEntry(
            name = "readme.txt", relativePath = "readme.txt", isDirectory = false, size = 100, modifiedAt = 1000L, extension = "txt"
        )

        // When
        val result = useCase(fileSource, testSource, paths)

        // Then
        assertTrue(result is Result.Ok)
        val scanResult = (result as Result.Ok).value
        assertEquals(0, scanResult.successCount)
        assertEquals(1, scanResult.skipCount)
    }
}
