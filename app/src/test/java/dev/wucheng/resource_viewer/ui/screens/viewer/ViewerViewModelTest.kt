package dev.wucheng.resource_viewer.ui.screens.viewer

import android.content.Context
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.datastore.FileBrowserPrefsStore
import dev.wucheng.resource_viewer.data.local.datastore.FileSortMode
import dev.wucheng.resource_viewer.data.local.datastore.FolderPrefs
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.filesource.FileSourceFactory
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ViewerViewModel 测试。
 * 测试页面管理、预加载状态和错误处理。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {
    private lateinit var mockContext: Context
    private lateinit var mockResourceRepository: ResourceRepository
    private lateinit var mockFilesystemRepository: FilesystemRepository
    private lateinit var mockFileSource: FileSource
    private lateinit var mockContentProvider: ContentProvider
    private lateinit var mockPrefsStore: FileBrowserPrefsStore
    private lateinit var viewModel: ViewerViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testResourceId = "test-resource-id"
    private val testResource = Resource(
        id = testResourceId,
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "Test Resource",
        type = dev.wucheng.resource_viewer.data.local.converter.ResourceType.FOLDER,
        organizationMode = dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.GALLERY,
        relativePath = "test/path",
        thumbnailPath = null,
        fileCount = 10,
        fileSize = 1024,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockResourceRepository = mockk()
        mockFilesystemRepository = mockk()
        mockFileSource = mockk()
        mockContentProvider = mockk()
        mockPrefsStore = mockk()

        // 设置默认行为
        every { mockContentProvider.pageCount } returns 10
        every { mockContentProvider.dispose() } just Runs
        // MixedFolderProvider 需要获取 Source 判断是否 SMB
        coEvery { mockFilesystemRepository.getSource(any()) } returns Result.Ok(localSource)

        viewModel = ViewerViewModel(
            resourceId = testResourceId,
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ===== RED: 测试初始状态 =====

    @Test
    fun `should emit loading state when initialized`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image1.jpg", relativePath = "test/path/image1.jpg",
                isDirectory = false, size = 1024,
                modifiedAt = System.currentTimeMillis(), extension = "jpg",
            ),
        )

        // Act
        viewModel.loadResource()
        advanceUntilIdle()

        // Assert - 初始状态应该是 loading（或快速完成时为 success）
        val state = viewModel.uiState.value
        assertTrue(state is ViewerUiState.Loading || state is ViewerUiState.Success)
    }

    @Test
    fun `should emit error state when resource not found`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(null)

        // Act
        viewModel.loadResource()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is ViewerUiState.Error)
    }

    @Test
    fun `should emit error state when resource load fails`() = runTest {
        // Arrange
        coEvery {
            mockResourceRepository.getById(testResourceId)
        } returns Result.Err(DomainError.DatabaseError("Database error"))

        // Act
        viewModel.loadResource()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is ViewerUiState.Error)
    }

    // ===== RED: 测试页面导航 =====

    @Test
    fun `should update current page when navigating to next page`() = runTest {
        // Arrange - 先加载资源
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image1.jpg",
                relativePath = "test/path/image1.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            ),
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image2.jpg",
                relativePath = "test/path/image2.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            ),
        )
        viewModel.loadResource()
        advanceUntilIdle()

        // Act
        viewModel.nextPage()
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.currentPage.value)
    }

    @Test
    fun `should not go beyond last page when navigating forward`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image1.jpg",
                relativePath = "test/path/image1.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            ),
        )
        viewModel.loadResource()
        advanceUntilIdle()

        // Act - 尝试超过最后一页
        repeat(20) { viewModel.nextPage() }
        advanceUntilIdle()

        // Assert - 应该停在最后一页（只有1页，index 0）
        val totalPages = viewModel.totalPages.value
        assertTrue(viewModel.currentPage.value < totalPages)
    }

    @Test
    fun `should not go before first page when navigating backward`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image1.jpg",
                relativePath = "test/path/image1.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            ),
        )
        viewModel.loadResource()
        advanceUntilIdle()

        // Act - 尝试在第一页时向前
        viewModel.previousPage()
        advanceUntilIdle()

        // Assert
        assertEquals(0, viewModel.currentPage.value)
    }

    @Test
    fun `should navigate to specific page when goToPage is called`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns (1..10).map { i ->
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image$i.jpg",
                relativePath = "test/path/image$i.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            )
        }
        viewModel.loadResource()
        advanceUntilIdle()

        // Act
        viewModel.goToPage(5)
        advanceUntilIdle()

        // Assert
        assertEquals(5, viewModel.currentPage.value)
    }

    // ===== RED: 测试资源名称 =====

    @Test
    fun `should expose resource name after loading`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)

        // Act
        viewModel.loadResource()
        advanceUntilIdle()

        // Assert
        assertEquals("Test Resource", viewModel.resourceName.value)
    }

    // ===== RED: 测试视频资源加载 =====

    private val localVideoResource = Resource(
        id = "video-local-id",
        sourceId = "local-source-1",
        sourceName = "Local Source",
        name = "Test Video",
        type = ResourceType.VIDEO,
        organizationMode = null,
        relativePath = "videos/movie.mp4",
        thumbnailPath = null,
        fileCount = null,
        fileSize = 1024L * 1024 * 100,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private val localSource = Source(
        id = "local-source-1",
        name = "Local Source",
        type = SourceType.LOCAL,
        rootPath = "/storage/emulated/0",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private val smbSource = Source(
        id = "smb-source-1",
        name = "SMB Source",
        type = SourceType.SMB,
        rootPath = "/share/videos",
        host = "192.168.1.100",
        port = 445,
        username = "testuser",
        domain = "WORKGROUP",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun `should emit ViewerItem Video when resource type is VIDEO and source is LOCAL`() = runTest {
        // Arrange
        val videoViewModel = ViewerViewModel(
            resourceId = "video-local-id",
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            ioDispatcher = testDispatcher,
        )
        coEvery { mockResourceRepository.getById("video-local-id") } returns Result.Ok(localVideoResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFilesystemRepository.getSource(any()) } returns Result.Ok(localSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "movie.mp4",
                relativePath = "videos/movie.mp4",
                isDirectory = false,
                size = 1024L * 1024 * 100,
                modifiedAt = System.currentTimeMillis(),
                extension = "mp4",
            ),
        )

        // Act
        videoViewModel.loadResource()
        advanceUntilIdle()

        // Assert
        val state = videoViewModel.uiState.value
        assertTrue("Expected Success state", state is ViewerUiState.Success)
        val success = state as ViewerUiState.Success
        assertEquals(1, success.items.size)
        assertTrue("Expected ViewerItem.Video", success.items[0] is ViewerItem.Video)
        val video = success.items[0] as ViewerItem.Video
        assertTrue("Expected LocalFile source", video.videoSource is VideoMediaSource.LocalFile)
    }

    @Test
    fun `should emit ViewerItem Video when resource type is VIDEO and source is SMB`() = runTest {
        // Arrange
        val videoViewModel = ViewerViewModel(
            resourceId = "video-local-id",
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            ioDispatcher = testDispatcher,
        )
        coEvery { mockResourceRepository.getById("video-local-id") } returns Result.Ok(localVideoResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFilesystemRepository.getSource(any()) } returns Result.Ok(smbSource)
        every { mockFilesystemRepository.getPassword(any()) } returns "testpass"
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "movie.mp4",
                relativePath = "videos/movie.mp4",
                isDirectory = false,
                size = 1024L * 1024 * 100,
                modifiedAt = System.currentTimeMillis(),
                extension = "mp4",
            ),
        )

        // Act
        videoViewModel.loadResource()
        advanceUntilIdle()

        // Assert
        val state = videoViewModel.uiState.value
        assertTrue("Expected Success state", state is ViewerUiState.Success)
        val success = state as ViewerUiState.Success
        assertEquals(1, success.items.size)
        assertTrue("Expected ViewerItem.Video", success.items[0] is ViewerItem.Video)
        val video = success.items[0] as ViewerItem.Video
        assertTrue("Expected SmbFile source", video.videoSource is VideoMediaSource.SmbFile)
    }

    @Test
    fun `loadFromSource should preserve current folder sort order`() = runTest {
        // Arrange
        val source = Source(
            id = "source-1",
            name = "Local Source",
            type = SourceType.LOCAL,
            rootPath = "/storage/emulated/0",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val sortedViewerViewModel = ViewerViewModel(
            resourceId = "__file__",
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            fileBrowserPrefsStore = mockPrefsStore,
            ioDispatcher = testDispatcher,
        )
        coEvery { mockFilesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { mockFilesystemRepository.getFileSource("source-1") } returns Result.Ok(mockFileSource)
        coEvery { mockPrefsStore.loadPrefs("source-1", "folder") } returns FolderPrefs(
            sortMode = FileSortMode.NAME_DESC,
        )
        coEvery { mockFileSource.listDirectory("folder") } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "a.jpg",
                relativePath = "folder/a.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = 1000L,
                extension = "jpg",
            ),
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "b.jpg",
                relativePath = "folder/b.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = 2000L,
                extension = "jpg",
            ),
        )

        // Act
        sortedViewerViewModel.loadFromSource("source-1", "folder/a.jpg")
        advanceUntilIdle()

        // Assert
        val state = sortedViewerViewModel.uiState.value
        assertTrue("Expected Success state", state is ViewerUiState.Success)
        val success = state as ViewerUiState.Success
        assertEquals(listOf("b.jpg", "a.jpg"), success.items.map { (it as ViewerItem.ImagePage).title })
        assertEquals(1, sortedViewerViewModel.currentPage.value)
    }

    @Test
    fun `loadFromSource should open readable archive file as image pages`() = runTest {
        val source = Source(
            id = "source-1",
            name = "Local Source",
            type = SourceType.LOCAL,
            rootPath = "/storage/emulated/0",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val archiveViewModel = ViewerViewModel(
            resourceId = "__file__",
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            ioDispatcher = testDispatcher,
        )
        coEvery { mockFilesystemRepository.getSource("source-1") } returns Result.Ok(source)
        coEvery { mockFilesystemRepository.getFileSource("source-1") } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.readFile("books/book.cbz") } returns zipBytes(
            "page2.jpg" to byteArrayOf(2),
            "page1.png" to byteArrayOf(1),
        )

        archiveViewModel.loadFromSource("source-1", "books/book.cbz")
        advanceUntilIdle()

        val state = archiveViewModel.uiState.value
        assertTrue("Expected Success state", state is ViewerUiState.Success)
        val success = state as ViewerUiState.Success
        assertEquals(2, success.items.size)
        assertEquals(listOf("png", "jpg"), success.items.map { (it as ViewerItem.ImagePage).extension })
    }

    // ===== RED: 测试 PDF 资源加载 =====

    private val pdfResource = dev.wucheng.resource_viewer.domain.model.Resource(
        id = "pdf-id",
        sourceId = "source-1",
        sourceName = "Test Source",
        name = "Test PDF",
        type = ResourceType.PDF,
        organizationMode = null,
        relativePath = "docs/test.pdf",
        thumbnailPath = null,
        fileCount = 10,
        fileSize = 1024,
        isAvailable = true,
        lastScannedAt = System.currentTimeMillis(),
        tags = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun `should emit error state when source is null for video resource`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById("video-local-id") } returns Result.Ok(localVideoResource)
        coEvery { mockFilesystemRepository.getSource(any()) } returns Result.Ok(null)

        // Act
        val videoViewModel = ViewerViewModel(
            resourceId = "video-local-id",
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            ioDispatcher = testDispatcher,
        )
        videoViewModel.loadResource()
        advanceUntilIdle()

        // Assert
        assertTrue(videoViewModel.uiState.value is ViewerUiState.Error)
    }

    @Test
    fun `should emit error state when source load fails for video resource`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById("video-local-id") } returns Result.Ok(localVideoResource)
        coEvery { mockFilesystemRepository.getSource(any()) } returns Result.Err(
            dev.wucheng.resource_viewer.domain.error.DomainError.DatabaseError("Source load failed")
        )

        // Act
        val videoViewModel = ViewerViewModel(
            resourceId = "video-local-id",
            resourceRepository = mockResourceRepository,
            filesystemRepository = mockFilesystemRepository,
            context = mockContext,
            ioDispatcher = testDispatcher,
        )
        videoViewModel.loadResource()
        advanceUntilIdle()

        // Assert
        assertTrue(videoViewModel.uiState.value is ViewerUiState.Error)
    }

    @Test
    fun `should not navigate to negative page when goToPage is called with negative value`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image1.jpg",
                relativePath = "test/path/image1.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            ),
        )
        viewModel.loadResource()
        advanceUntilIdle()

        // Act
        viewModel.goToPage(-1)

        // Assert
        assertEquals(0, viewModel.currentPage.value)
    }

    @Test
    fun `should not navigate beyond totalPages when goToPage is called with large value`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Ok(mockFileSource)
        coEvery { mockFileSource.listDirectory(any()) } returns listOf(
            dev.wucheng.resource_viewer.domain.model.FileEntry(
                name = "image1.jpg",
                relativePath = "test/path/image1.jpg",
                isDirectory = false,
                size = 1024,
                modifiedAt = System.currentTimeMillis(),
                extension = "jpg",
            ),
        )
        viewModel.loadResource()
        advanceUntilIdle()

        // Act
        viewModel.goToPage(100)

        // Assert
        assertEquals(0, viewModel.currentPage.value)
    }

    @Test
    fun `should emit error when getFileSource fails for content provider resource`() = runTest {
        // Arrange
        coEvery { mockResourceRepository.getById(testResourceId) } returns Result.Ok(testResource)
        coEvery { mockFilesystemRepository.getFileSource(any()) } returns Result.Err(
            dev.wucheng.resource_viewer.domain.error.DomainError.SourceUnreachableError("Source unreachable")
        )

        // Act
        viewModel.loadResource()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value is ViewerUiState.Error)
    }

    // ===== RED: 测试 dispose =====
    // 注意：onCleared 是 protected 方法，无法直接测试
    // 在实际集成测试中验证 dispose 行为

    private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
