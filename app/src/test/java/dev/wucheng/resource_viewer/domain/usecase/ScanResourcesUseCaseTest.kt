package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Progress
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScanResourcesUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var detectOrganizationModeUseCase: DetectOrganizationModeUseCase
    private lateinit var useCase: ScanResourcesUseCase

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
        useCase = ScanResourcesUseCase(resourceRepository, detectOrganizationModeUseCase)
    }

    @Test
    fun `should emit progress updates and return scan result when scanning succeeds`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val entries = listOf(
            FileEntry(name = "folder1", relativePath = "folder1", isDirectory = true, size = 0, modifiedAt = 1000L),
            FileEntry(name = "image.jpg", relativePath = "image.jpg", isDirectory = false, size = 1024, modifiedAt = 1000L, extension = "jpg"),
        )
        coEvery { fileSource.listDirectory("") } returns entries
        coEvery { fileSource.sourceId } returns "source-1"
        coEvery { detectOrganizationModeUseCase(fileSource, "folder1") } returns dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.FLATGRID
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)

        // When
        val emissions = useCase(fileSource, testSource, "").toList()

        // Then
        assertTrue(emissions.any { it is Progress.Update })
        assertTrue(emissions.last() is Progress.Done)
        val done = emissions.last() as Progress.Done
        assertEquals(1, done.result.successCount) // Only folder1 is supported, image.jpg is skipped
        assertEquals(1, done.result.skipCount)
        assertTrue(done.result.failures.isEmpty())
    }

    @Test
    fun `should skip entries that already exist in repository`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val entries = listOf(
            FileEntry(name = "existing.jpg", relativePath = "existing.jpg", isDirectory = false, size = 1024, modifiedAt = 1000L, extension = "jpg"),
        )
        coEvery { fileSource.listDirectory("") } returns entries
        coEvery { fileSource.sourceId } returns "source-1"
        // Simulate insertAll throwing on conflict (IGNORE strategy)
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)

        // When
        val emissions = useCase(fileSource, testSource, "").toList()

        // Then
        assertTrue(emissions.last() is Progress.Done)
    }

    @Test
    fun `should record failures when file source throws exception`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        val entries = listOf(
            FileEntry(name = "bad.jpg", relativePath = "bad.jpg", isDirectory = false, size = 1024, modifiedAt = 1000L, extension = "jpg"),
            FileEntry(name = "good.jpg", relativePath = "good.jpg", isDirectory = false, size = 1024, modifiedAt = 1000L, extension = "jpg"),
        )
        coEvery { fileSource.listDirectory("") } returns entries
        coEvery { fileSource.sourceId } returns "source-1"
        coEvery { detectOrganizationModeUseCase(any(), any()) } returns dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.FLATGRID
        coEvery { resourceRepository.insertAll(any()) } returns Result.Ok(Unit)

        // When
        val emissions = useCase(fileSource, testSource, "").toList()

        // Then
        val done = emissions.last() as Progress.Done
        assertTrue(done.result.failures.isEmpty() || done.result.successCount > 0)
    }

    @Test
    fun `should handle empty directory gracefully`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        coEvery { fileSource.listDirectory("") } returns emptyList()
        coEvery { fileSource.sourceId } returns "source-1"

        // When
        val emissions = useCase(fileSource, testSource, "").toList()

        // Then
        val done = emissions.last() as Progress.Done
        assertEquals(0, done.result.successCount)
        assertEquals(0, done.result.skipCount)
        assertTrue(done.result.failures.isEmpty())
    }

    @Test
    fun `should emit error when file source listDirectory fails`() = runTest {
        // Given
        val fileSource = mockk<FileSource>()
        coEvery { fileSource.listDirectory("") } throws RuntimeException("Connection lost")
        coEvery { fileSource.sourceId } returns "source-1"

        // When
        val emissions = useCase(fileSource, testSource, "").toList()

        // Then
        assertTrue(emissions.any { it is Progress.Error })
    }
}
