package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DetectOrganizationModeUseCaseTest {
    private lateinit var useCase: DetectOrganizationModeUseCase
    private lateinit var mockFileSource: FileSource

    @Before
    fun setup() {
        useCase = DetectOrganizationModeUseCase()
        mockFileSource = mockk(relaxed = true)
    }

    @Test
    fun `should return FLATGRID when folder contains only images`() =
        runTest {
            val entries =
                listOf(
                    FileEntry("photo1.jpg", "photo1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
                    FileEntry("photo2.png", "photo2.png", false, 2048, System.currentTimeMillis(), "png"),
                    FileEntry("photo3.webp", "photo3.webp", false, 3072, System.currentTimeMillis(), "webp"),
                )
            coEvery { mockFileSource.listDirectory(any()) } returns entries

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.FLATGRID, result)
        }

    @Test
    fun `should return CHAPTER when folder contains subfolders with images`() =
        runTest {
            val rootEntries =
                listOf(
                    FileEntry("chapter1", "chapter1", true, 0, System.currentTimeMillis(), ""),
                    FileEntry("chapter2", "chapter2", true, 0, System.currentTimeMillis(), ""),
                )
            val chapter1Entries =
                listOf(
                    FileEntry("photo1.jpg", "chapter1/photo1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
                )
            coEvery { mockFileSource.listDirectory("/test/path") } returns rootEntries
            coEvery { mockFileSource.listDirectory("/test/path/chapter1") } returns chapter1Entries
            coEvery { mockFileSource.listDirectory("/test/path/chapter2") } returns emptyList()

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.CHAPTER, result)
        }

    @Test
    fun `should return FLATGRID when folder contains mixed file types`() =
        runTest {
            val entries =
                listOf(
                    FileEntry("photo1.jpg", "photo1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
                    FileEntry("document.pdf", "document.pdf", false, 4096, System.currentTimeMillis(), "pdf"),
                    FileEntry("video.mp4", "video.mp4", false, 8192, System.currentTimeMillis(), "mp4"),
                )
            coEvery { mockFileSource.listDirectory(any()) } returns entries

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.FLATGRID, result)
        }

    @Test
    fun `should return FLATGRID when folder is empty`() =
        runTest {
            coEvery { mockFileSource.listDirectory(any()) } returns emptyList()

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.FLATGRID, result)
        }

    @Test
    fun `should return CHAPTER when folder has subfolders but no direct images`() =
        runTest {
            val rootEntries =
                listOf(
                    FileEntry("chapter1", "chapter1", true, 0, System.currentTimeMillis(), ""),
                    FileEntry("readme.txt", "readme.txt", false, 100, System.currentTimeMillis(), "txt"),
                )
            val chapter1Entries =
                listOf(
                    FileEntry("photo1.jpg", "chapter1/photo1.jpg", false, 1024, System.currentTimeMillis(), "jpg"),
                )
            coEvery { mockFileSource.listDirectory("/test/path") } returns rootEntries
            coEvery { mockFileSource.listDirectory("/test/path/chapter1") } returns chapter1Entries

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.CHAPTER, result)
        }

    @Test
    fun `should return FLATGRID when subfolders exist but contain no images`() =
        runTest {
            val rootEntries =
                listOf(
                    FileEntry("docs", "docs", true, 0, System.currentTimeMillis(), ""),
                    FileEntry("video.mp4", "video.mp4", false, 8192, System.currentTimeMillis(), "mp4"),
                )
            val docsEntries =
                listOf(
                    FileEntry("readme.txt", "docs/readme.txt", false, 100, System.currentTimeMillis(), "txt"),
                    FileEntry("data.csv", "docs/data.csv", false, 200, System.currentTimeMillis(), "csv"),
                )
            coEvery { mockFileSource.listDirectory("/test/path") } returns rootEntries
            coEvery { mockFileSource.listDirectory("/test/path/docs") } returns docsEntries

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.FLATGRID, result)
        }

    @Test
    fun `should return FLATGRID when folder contains only non-image media files`() =
        runTest {
            val entries =
                listOf(
                    FileEntry("video1.mp4", "video1.mp4", false, 8192, System.currentTimeMillis(), "mp4"),
                    FileEntry("document.pdf", "document.pdf", false, 4096, System.currentTimeMillis(), "pdf"),
                    FileEntry("clip.avi", "clip.avi", false, 2048, System.currentTimeMillis(), "avi"),
                )
            coEvery { mockFileSource.listDirectory(any()) } returns entries

            val result = useCase.invoke(mockFileSource, "/test/path")

            assertEquals(OrganizationMode.FLATGRID, result)
        }
}
