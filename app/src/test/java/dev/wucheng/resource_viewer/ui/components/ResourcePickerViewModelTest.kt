package dev.wucheng.resource_viewer.ui.components

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class ResourcePickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockFilesystemRepo: FilesystemRepository
    private lateinit var mockSourceRepo: SourceRepository

    private val testSource = Source(
        id = "src-1",
        name = "Test Source",
        type = SourceType.LOCAL,
        rootPath = "/test",
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockFilesystemRepo = mockk()
        mockSourceRepo = mockk()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun setupMocks(
        source: Source? = testSource,
        rootEntries: List<FileEntry> = emptyList(),
        subEntries: Map<String, List<FileEntry>> = emptyMap(),
    ) {
        coEvery { mockSourceRepo.getSourceById("src-1") } returns Result.Ok(source)
        coEvery { mockFilesystemRepo.listDirectory(any(), any()) } coAnswers {
            val path = secondArg<String>()
            if (path == "/root") Result.Ok(rootEntries)
            else subEntries[path]?.let { Result.Ok(it) }
                ?: Result.Ok(emptyList())
        }
    }

    @Test
    fun `should load tree from filesystem repository`() = runTest {
        val entries = listOf(
            FileEntry("folder1", "folder1", isDirectory = true, size = 0, modifiedAt = 0),
            FileEntry("file1.jpg", "file1.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
        )
        setupMocks(rootEntries = entries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        val tree = viewModel.treeNodes.value
        assertEquals(2, tree.size)
        assertEquals("folder1", tree[0].name)
        assertEquals("file1.jpg", tree[1].name)
        // Paths include parent path
        assertEquals("/root/folder1", tree[0].relativePath)
        assertEquals("/root/file1.jpg", tree[1].relativePath)
    }

    @Test
    fun `should emit error state when source not found`() = runTest {
        coEvery { mockSourceRepo.getSourceById("src-1") } returns Result.Ok(null)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ResourcePickerUiState.Error)
    }

    @Test
    fun `should emit error when source fetch fails`() = runTest {
        coEvery { mockSourceRepo.getSourceById("src-1") } returns Result.Err(
            DomainError.DatabaseError("fail")
        )

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ResourcePickerUiState.Error)
    }

    @Test
    fun `should toggle expand state for directory node`() = runTest {
        val entries = listOf(
            FileEntry("folder1", "folder1", isDirectory = true, size = 0, modifiedAt = 0),
        )
        val subEntries = mapOf(
            "/root/folder1" to listOf(
                FileEntry("sub.jpg", "sub.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
            ),
        )
        setupMocks(rootEntries = entries, subEntries = subEntries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        assertFalse(viewModel.treeNodes.value[0].isExpanded)

        viewModel.toggleExpand("/root/folder1")
        advanceUntilIdle()

        assertTrue(viewModel.treeNodes.value[0].isExpanded)
        assertEquals(1, viewModel.treeNodes.value[0].children.size)
    }

    @Test
    fun `should toggle check state for node`() = runTest {
        val entries = listOf(
            FileEntry("file1.jpg", "file1.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
        )
        setupMocks(rootEntries = entries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        assertFalse(viewModel.treeNodes.value[0].isChecked)

        viewModel.toggleCheck("/root/file1.jpg")
        advanceUntilIdle()

        assertTrue(viewModel.treeNodes.value[0].isChecked)
        assertEquals(1, viewModel.selectedCount.value)
    }

    @Test
    fun `should select all direct children of a directory`() = runTest {
        val entries = listOf(
            FileEntry("folder1", "folder1", isDirectory = true, size = 0, modifiedAt = 0),
        )
        val subEntries = mapOf(
            "/root/folder1" to listOf(
                FileEntry("a.jpg", "a.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
                FileEntry("b.jpg", "b.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
            ),
        )
        setupMocks(rootEntries = entries, subEntries = subEntries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        viewModel.toggleExpand("/root/folder1")
        advanceUntilIdle()

        viewModel.selectAllChildren("/root/folder1")
        advanceUntilIdle()

        val folder = viewModel.treeNodes.value[0]
        assertTrue(folder.children[0].isChecked)
        assertTrue(folder.children[1].isChecked)
        assertEquals(2, viewModel.selectedCount.value)
    }

    @Test
    fun `should deselect all children when all already checked`() = runTest {
        val entries = listOf(
            FileEntry("folder1", "folder1", isDirectory = true, size = 0, modifiedAt = 0),
        )
        val subEntries = mapOf(
            "/root/folder1" to listOf(
                FileEntry("a.jpg", "a.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
                FileEntry("b.jpg", "b.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
            ),
        )
        setupMocks(rootEntries = entries, subEntries = subEntries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        viewModel.toggleExpand("/root/folder1")
        advanceUntilIdle()

        viewModel.selectAllChildren("/root/folder1")
        advanceUntilIdle()
        assertEquals(2, viewModel.selectedCount.value)

        viewModel.selectAllChildren("/root/folder1")
        advanceUntilIdle()
        assertEquals(0, viewModel.selectedCount.value)
    }

    @Test
    fun `should get selected file entries`() = runTest {
        val entries = listOf(
            FileEntry("file1.jpg", "file1.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
            FileEntry("file2.jpg", "file2.jpg", isDirectory = false, size = 200, modifiedAt = 0, extension = "jpg"),
        )
        setupMocks(rootEntries = entries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        viewModel.toggleCheck("/root/file1.jpg")
        advanceUntilIdle()

        val selected = viewModel.getSelectedEntries()
        assertEquals(1, selected.size)
        assertEquals("/root/file1.jpg", selected[0].relativePath)
    }

    @Test
    fun `should set root name from source name`() = runTest {
        setupMocks(rootEntries = emptyList())

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        assertEquals("Test Source", viewModel.rootName.value)
    }

    @Test
    fun `should filter out unrecognized file types`() = runTest {
        val entries = listOf(
            FileEntry("photo.jpg", "photo.jpg", isDirectory = false, size = 100, modifiedAt = 0, extension = "jpg"),
            FileEntry("readme.txt", "readme.txt", isDirectory = false, size = 50, modifiedAt = 0, extension = "txt"),
            FileEntry("video.mp4", "video.mp4", isDirectory = false, size = 500, modifiedAt = 0, extension = "mp4"),
        )
        setupMocks(rootEntries = entries)

        val viewModel = ResourcePickerViewModel(mockFilesystemRepo, mockSourceRepo)
        viewModel.loadTree("src-1", "/root")
        advanceUntilIdle()

        val tree = viewModel.treeNodes.value
        assertEquals(2, tree.size)
        assertEquals("photo.jpg", tree[0].name)
        assertEquals("video.mp4", tree[1].name)
    }
}
