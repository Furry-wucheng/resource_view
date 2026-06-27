package dev.wucheng.resource_viewer.ui.screens.sources

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.usecase.BatchAddResourcesUseCase
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileBrowserViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var filesystemRepository: FilesystemRepository
    private lateinit var batchAddResourcesUseCase: BatchAddResourcesUseCase
    private lateinit var tagRepository: TagRepository
    private lateinit var fileSource: FileSource
    private lateinit var viewModel: FileBrowserViewModel

    private val source = Source(
        id = "source-1",
        name = "Local",
        type = SourceType.LOCAL,
        rootPath = "/root",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        filesystemRepository = mockk()
        batchAddResourcesUseCase = mockk()
        tagRepository = mockk(relaxed = true)
        fileSource = mockk()
        viewModel = FileBrowserViewModel("source-1", filesystemRepository, batchAddResourcesUseCase, tagRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load should populate current directory entries`() = runTest {
        val entries = listOf(
            FileEntry("folder", "folder", true, 0, 1L),
            FileEntry("book.pdf", "book.pdf", false, 100, 1L, "pdf"),
        )
        coEvery { filesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { filesystemRepository.listDirectory(source, "") } returns Result.Ok(entries)

        viewModel.load()

        assertEquals(source, viewModel.uiState.value.source)
        assertEquals(listOf("folder", "book.pdf"), viewModel.uiState.value.entries.map { it.name })
    }

    @Test
    fun `goUp should return false at root`() = runTest {
        coEvery { filesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { filesystemRepository.listDirectory(source, "") } returns Result.Ok(emptyList())

        viewModel.load()
        val result = viewModel.goUp()

        assertFalse(result)
    }

    @Test
    fun `goUp should navigate to parent`() = runTest {
        coEvery { filesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { filesystemRepository.listDirectory(source, "") } returns Result.Ok(emptyList())
        coEvery { filesystemRepository.listDirectory(source, "a") } returns Result.Ok(emptyList())
        coEvery { filesystemRepository.listDirectory(source, "a/b") } returns Result.Ok(emptyList())

        viewModel.load()
        viewModel.openDirectory("a/b")
        assertEquals("a/b", viewModel.uiState.value.currentPath)

        val result = viewModel.goUp()
        assertTrue(result)
        assertEquals("a", viewModel.uiState.value.currentPath)
    }

    @Test
    fun `enterMultiSelect and exitMultiSelect should toggle mode`() = runTest {
        coEvery { filesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { filesystemRepository.listDirectory(source, "") } returns Result.Ok(emptyList())

        viewModel.load()

        assertFalse(viewModel.uiState.value.isMultiSelectMode)

        viewModel.enterMultiSelect()
        assertTrue(viewModel.uiState.value.isMultiSelectMode)

        viewModel.toggleSelection("a.pdf")
        assertEquals(setOf("a.pdf"), viewModel.uiState.value.selectedPaths)

        viewModel.exitMultiSelect()
        assertFalse(viewModel.uiState.value.isMultiSelectMode)
        assertTrue(viewModel.uiState.value.selectedPaths.isEmpty())
    }

    @Test
    fun `openDirectory should navigate into directory`() = runTest {
        coEvery { filesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { filesystemRepository.listDirectory(source, "") } returns Result.Ok(emptyList())
        coEvery { filesystemRepository.listDirectory(source, "sub") } returns Result.Ok(emptyList())

        viewModel.load()
        viewModel.openDirectory("sub")

        assertEquals("sub", viewModel.uiState.value.currentPath)
        assertEquals(listOf("sub"), viewModel.uiState.value.pathSegments)
    }

    @Test
    fun `confirmBatchAdd should call batch add use case`() = runTest {
        val entries = listOf(FileEntry("book.pdf", "book.pdf", false, 100, 1L, "pdf"))
        coEvery { filesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { filesystemRepository.listDirectory(source, "") } returns Result.Ok(entries)
        coEvery { batchAddResourcesUseCase(fileSource, source, listOf("book.pdf"), any(), any()) } returns
            Result.Ok(ScanResult(successCount = 1, skipCount = 0, failures = emptyList()))

        viewModel.load()
        viewModel.enterMultiSelect()
        viewModel.toggleSelection("book.pdf")
        viewModel.showBatchAddDialog()
        viewModel.confirmBatchAdd(null, emptyList())

        coVerify { batchAddResourcesUseCase(fileSource, source, listOf("book.pdf"), any(), any()) }
        assertEquals(1, viewModel.uiState.value.lastAddResult?.successCount)
        assertTrue(viewModel.uiState.value.selectedPaths.isEmpty())
    }
}
