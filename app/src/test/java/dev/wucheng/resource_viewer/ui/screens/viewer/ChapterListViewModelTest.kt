package dev.wucheng.resource_viewer.ui.screens.viewer

import android.graphics.Bitmap
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailLoadManager
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailSearchPolicy
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val resourceRepository = mockk<ResourceRepository>()
    private val filesystemRepository = mockk<FilesystemRepository>()
    private val thumbnailLoadManager = mockk<ThumbnailLoadManager>(relaxed = true)
    private val fileSource = mockk<FileSource>()
    private val looseFile = FileEntry("cover.jpg", "root/cover.jpg", false, 1024, 1, "jpg")
    private val resource = Resource(
        id = "resource-1",
        sourceId = "source-1",
        sourceName = "Source",
        name = "Book",
        type = ResourceType.FOLDER,
        organizationMode = OrganizationMode.CHAPTER_GALLERY,
        relativePath = "root",
        thumbnailPath = null,
        fileCount = 1,
        fileSize = null,
        isAvailable = true,
        lastScannedAt = 1,
        createdAt = 1,
        updatedAt = 1,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loose file thumbnail should use shared thumbnail manager`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { resourceRepository.getById("resource-1") } returns Result.Ok(resource)
        coEvery { filesystemRepository.getFileSource("source-1") } returns Result.Ok(fileSource)
        coEvery { fileSource.listDirectory("root") } returns listOf(looseFile)
        coEvery {
            thumbnailLoadManager.load("source-1", looseFile, ThumbnailSearchPolicy.DIRECT_CHILD)
        } returns bitmap
        val viewModel = ChapterListViewModel(
            "resource-1",
            resourceRepository,
            filesystemRepository,
            thumbnailLoadManager,
        )

        viewModel.loadChapters()
        val result = viewModel.loadLooseFileThumbnail(looseFile)

        assertSame(bitmap, result)
        coVerify(exactly = 1) {
            thumbnailLoadManager.load("source-1", looseFile, ThumbnailSearchPolicy.DIRECT_CHILD)
        }
    }
}
