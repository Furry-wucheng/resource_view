package dev.wucheng.resource_viewer.ui.screens.sources

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.remote.smb.SmbClientWrapper
import dev.wucheng.resource_viewer.data.remote.smb.SmbConnectionException
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.shared.filesource.FileSourceFactory
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceListViewModelTest {

    private lateinit var mockSourceRepository: SourceRepository
    private lateinit var mockFilesystemRepository: FilesystemRepository
    private lateinit var mockResourceRepository: ResourceRepository
    private lateinit var mockSmbClientWrapper: SmbClientWrapper
    private lateinit var viewModel: SourceListViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSourceRepository = mockk(relaxed = true)
        mockFilesystemRepository = mockk(relaxed = true)
        mockResourceRepository = mockk(relaxed = true)
        mockSmbClientWrapper = mockk(relaxed = true)
        viewModel = SourceListViewModel(
            sourceRepository = mockSourceRepository,
            filesystemRepository = mockFilesystemRepository,
            resourceRepository = mockResourceRepository,
            smbClientWrapper = mockSmbClientWrapper,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() = runTest {
        // Given
        every { mockSourceRepository.getAllSources() } returns flowOf(emptyList())

        // When
        viewModel.loadSources()

        // Then
        assertTrue(viewModel.uiState.value.sources.isEmpty())
    }

    @Test
    fun `loadSources should update state with sources`() = runTest {
        // Given
        val sources = listOf(
            Source(
                id = "1",
                name = "Local Source",
                type = SourceType.LOCAL,
                rootPath = "/path",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            Source(
                id = "2",
                name = "SMB Source",
                type = SourceType.SMB,
                rootPath = "/share",
                host = "192.168.1.100",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        every { mockSourceRepository.getAllSources() } returns flowOf(sources)

        // When
        viewModel.loadSources()

        // Then
        assertEquals(2, viewModel.uiState.value.sources.size)
    }

    @Test
    fun `showAddSmbDialog should update dialog state`() = runTest {
        // When
        viewModel.showAddSmbDialog()

        // Then
        assertTrue(viewModel.uiState.value.showAddSmbDialog)
    }

    @Test
    fun `hideAddSmbDialog should reset dialog state`() = runTest {
        // Given
        viewModel.showAddSmbDialog()

        // When
        viewModel.hideAddSmbDialog()

        // Then
        assertFalse(viewModel.uiState.value.showAddSmbDialog)
    }

    @Test
    fun `updateSmbForm should update form data`() = runTest {
        // When
        viewModel.updateSmbForm(host = "192.168.1.100", port = 445)

        // Then
        assertEquals("192.168.1.100", viewModel.uiState.value.smbForm.host)
        assertEquals(445, viewModel.uiState.value.smbForm.port)
    }

    @Test
    fun `testSmbConnection should show success when connection succeeds`() = runTest {
        // Given
        viewModel.updateSmbForm(
            host = "192.168.1.100",
            port = 445,
            username = "user",
            password = "pass",
            shareName = "share"
        )
        every {
            mockSmbClientWrapper.testConnection(any(), any(), any(), any(), any(), any())
        } just Runs

        // When
        viewModel.testSmbConnection()

        // Then
        assertTrue(viewModel.uiState.value.testConnectionSuccess == true)
        assertNull(viewModel.uiState.value.testConnectionError)
    }

    @Test
    fun `testSmbConnection should show error when connection fails`() = runTest {
        // Given
        viewModel.updateSmbForm(
            host = "192.168.1.100",
            port = 445,
            username = "user",
            password = "pass",
            shareName = "share"
        )
        every {
            mockSmbClientWrapper.testConnection(any(), any(), any(), any(), any(), any())
        } throws SmbConnectionException("Connection failed")

        // When
        viewModel.testSmbConnection()

        // Then
        assertFalse(viewModel.uiState.value.testConnectionSuccess == true)
        assertNotNull(viewModel.uiState.value.testConnectionError)
    }

    @Test
    fun `addSmbSource should save source when form is valid`() = runTest {
        // Given
        viewModel.updateSmbForm(
            name = "Test SMB",
            host = "192.168.1.100",
            port = 445,
            username = "user",
            password = "pass",
            shareName = "share"
        )
        coEvery { mockSourceRepository.insert(any()) } returns Result.Ok(Unit)
        every { mockSourceRepository.putPassword(any(), any()) } just Runs

        // When
        viewModel.addSmbSource()

        // Then
        coVerify { mockSourceRepository.insert(any()) }
        verify { mockSourceRepository.putPassword(any(), "pass") }
        assertFalse(viewModel.uiState.value.showAddSmbDialog)
    }

    @Test
    fun `updateSmbSource should evict cached file source after saving config`() = runTest {
        mockkObject(FileSourceFactory)
        try {
            // Given
            every { FileSourceFactory.evict("smb-1") } just Runs
            val source = Source(
                id = "smb-1",
                name = "Old SMB",
                type = SourceType.SMB,
                rootPath = "/share",
                host = "192.168.1.10",
                port = 445,
                username = "old-user",
                passwordStored = true,
                createdAt = 1000L,
                updatedAt = 1000L,
            )
            every { mockSourceRepository.getPassword("smb-1") } returns "old-pass"
            coEvery { mockSourceRepository.update(any()) } returns Result.Ok(Unit)
            every { mockSourceRepository.putPassword("smb-1", "new-pass") } just Runs

            // When
            viewModel.showEditSmbDialog(source)
            viewModel.updateSmbForm(
                host = "192.168.1.20",
                username = "new-user",
                password = "new-pass",
            )
            viewModel.updateSmbSource()

            // Then
            coVerify {
                mockSourceRepository.update(
                    match {
                        it.id == "smb-1" &&
                            it.host == "192.168.1.20" &&
                            it.username == "new-user"
                    }
                )
            }
            verify { mockSourceRepository.putPassword("smb-1", "new-pass") }
            verify { FileSourceFactory.evict("smb-1") }
            assertFalse(viewModel.uiState.value.showEditSmbDialog)
        } finally {
            unmockkObject(FileSourceFactory)
        }
    }

    @Test
    fun `addLocalSource should save local source when form is valid`() = runTest {
        viewModel.updateLocalForm(
            name = "Local Comics",
            rootPath = "/storage/emulated/0/Comics",
        )
        coEvery { mockSourceRepository.insert(any()) } returns Result.Ok(Unit)

        viewModel.addLocalSource()

        coVerify {
            mockSourceRepository.insert(
                match {
                    it.name == "Local Comics" &&
                        it.type == SourceType.LOCAL &&
                        it.rootPath == "/storage/emulated/0/Comics"
                }
            )
        }
        assertFalse(viewModel.uiState.value.showAddLocalDialog)
    }

    @Test
    fun `addLocalSource should show error when root path is empty`() = runTest {
        viewModel.updateLocalForm(name = "Local Comics", rootPath = "")

        viewModel.addLocalSource()

        assertNotNull(viewModel.uiState.value.error)
        coVerify(exactly = 0) { mockSourceRepository.insert(any()) }
    }

    @Test
    fun `addSmbSource should show error when name is empty`() = runTest {
        // Given
        viewModel.updateSmbForm(
            name = "",
            host = "192.168.1.100",
            port = 445,
            username = "user",
            password = "pass",
            shareName = "share"
        )

        // When
        viewModel.addSmbSource()

        // Then
        assertNotNull(viewModel.uiState.value.error)
        coVerify(exactly = 0) { mockSourceRepository.insert(any()) }
    }

    @Test
    fun `deleteSource should remove source from repository`() = runTest {
        // Given
        coEvery { mockSourceRepository.deleteById("1") } returns Result.Ok(Unit)
        every { mockSourceRepository.removePassword("1") } just Runs

        // When
        viewModel.deleteSource("1")

        // Then
        coVerify { mockSourceRepository.deleteById("1") }
        verify { mockSourceRepository.removePassword("1") }
    }

    @Test
    fun `toggleSourceEnabled should update source availability`() = runTest {
        // Given
        val source = Source(
            id = "1",
            name = "Test",
            type = SourceType.LOCAL,
            rootPath = "/path",
            enabled = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        coEvery { mockSourceRepository.update(any()) } returns Result.Ok(Unit)

        // When
        viewModel.toggleSourceEnabled(source)

        // Then
        coVerify { mockSourceRepository.update(any()) }
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        // Given
        viewModel.updateSmbForm(name = "") // This sets an error
        viewModel.addSmbSource()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }
}
