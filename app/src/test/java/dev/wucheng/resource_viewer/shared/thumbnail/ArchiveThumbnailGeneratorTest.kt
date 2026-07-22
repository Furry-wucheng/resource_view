package dev.wucheng.resource_viewer.shared.thumbnail

import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchiveThumbnailGeneratorTest {
    private val generator = ArchiveThumbnailGenerator()

    @Test fun `canHandle should only accept archive resource type`() {
        assertTrue(generator.canHandle(ResourceType.ARCHIVE))
        assertFalse(generator.canHandle(ResourceType.FOLDER))
        assertFalse(generator.canHandle(ResourceType.PDF))
        assertFalse(generator.canHandle(ResourceType.VIDEO))
    }

    @Test fun `generate returns null for unsupported rar archive`() = runTest {
        val source = mockk<FileSource>()
        val resource = Resource(
            id = "archive-1",
            sourceId = "source-1",
            sourceName = "Source",
            name = "book.rar",
            type = ResourceType.ARCHIVE,
            organizationMode = null,
            relativePath = "book.rar",
            thumbnailPath = null,
            fileCount = null,
            fileSize = 10,
            isAvailable = true,
            lastScannedAt = 0,
            tags = emptyList(),
            createdAt = 0,
            updatedAt = 0,
        )
        coEvery { source.readFile("book.rar") } returns byteArrayOf(1, 2, 3)

        assertNull(generator.generate(resource, source, File("build/tmp/archive-thumb-test")))
    }

}
