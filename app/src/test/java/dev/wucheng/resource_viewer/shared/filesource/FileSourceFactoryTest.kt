package dev.wucheng.resource_viewer.shared.filesource

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.domain.model.Source
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * FileSourceFactory 测试。
 * 测试工厂类根据 SourceType 创建正确的 FileSource 实现。
 */
class FileSourceFactoryTest {

    private fun createTestSource(
        type: SourceType,
        rootPath: String = "/test/path",
        host: String? = null,
        port: Int? = null,
        username: String? = null,
        domain: String? = null,
    ) = Source(
        id = "test-source-id",
        name = "Test Source",
        type = type,
        rootPath = rootPath,
        host = host,
        port = port,
        username = username,
        domain = domain,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun `should create SmbFileSource for SMB type`() {
        val source = createTestSource(
            type = SourceType.SMB,
            rootPath = "/share",
            host = "192.168.1.100",
            port = 445,
            username = "testuser",
            domain = "WORKGROUP",
        )
        val password = "testpass"

        val fileSource = FileSourceFactory.create(source, password)

        assertNotNull(fileSource)
        assertTrue(fileSource is SmbFileSource)
        assertEquals("test-source-id", fileSource.sourceId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception when creating SMB source without password`() {
        val source = createTestSource(
            type = SourceType.SMB,
            rootPath = "/share",
            host = "192.168.1.100",
        )

        FileSourceFactory.create(source, null)
    }

    @Test
    fun `should create SmbFileSource when creating SMB source with empty password`() {
        val source = createTestSource(
            type = SourceType.SMB,
            rootPath = "/share",
            host = "192.168.1.100",
        )

        // Empty password is allowed, only null password throws exception
        val fileSource = FileSourceFactory.create(source, "")
        assertNotNull(fileSource)
        assertTrue(fileSource is SmbFileSource)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `should throw exception for FTP type`() {
        val source = createTestSource(type = SourceType.FTP)

        FileSourceFactory.create(source)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `should throw exception for WebDAV type`() {
        val source = createTestSource(type = SourceType.WEBDAV)

        FileSourceFactory.create(source)
    }

    @Test
    fun `should create LocalFileSource for LOCAL type`() {
        val source = createTestSource(type = SourceType.LOCAL)

        val fileSource = FileSourceFactory.create(source)

        assertNotNull(fileSource)
        // LocalFileSource is private, but we can verify it's not null and has correct sourceId
        assertEquals("test-source-id", fileSource.sourceId)
    }

    @Test
    fun `should create SmbFileSource with correct sourceId`() {
        val source = createTestSource(
            type = SourceType.SMB,
            rootPath = "/share",
            host = "192.168.1.100",
        )
        val password = "testpass"

        val fileSource = FileSourceFactory.create(source, password)

        assertEquals("test-source-id", fileSource.sourceId)
    }

    @Test
    fun `should create LocalFileSource with correct sourceId`() {
        val source = createTestSource(type = SourceType.LOCAL)

        val fileSource = FileSourceFactory.create(source)

        assertEquals("test-source-id", fileSource.sourceId)
    }

    @Test
    fun `local file source should list stat and read files under root`() = kotlinx.coroutines.test.runTest {
        val root = Files.createTempDirectory("resource-viewer-local-source").toFile()
        try {
            File(root, "b.txt").writeText("hello")
            File(root, "a").mkdirs()

            val source = createTestSource(type = SourceType.LOCAL, rootPath = root.absolutePath)
            val fileSource = FileSourceFactory.create(source)

            val entries = fileSource.listDirectory("")
            assertEquals(listOf("a", "b.txt"), entries.map { it.name })
            assertTrue(entries.first { it.name == "a" }.isDirectory)
            assertEquals("hello", fileSource.readFile("b.txt").decodeToString())
            assertEquals("b.txt", fileSource.stat("b.txt")?.relativePath)
            assertTrue(fileSource.testConnection())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `local file source should reject paths escaping root`() = kotlinx.coroutines.test.runTest {
        val root = Files.createTempDirectory("resource-viewer-local-source").toFile()
        try {
            val source = createTestSource(type = SourceType.LOCAL, rootPath = root.absolutePath)
            val fileSource = FileSourceFactory.create(source)

            fileSource.stat("../outside.txt")
        } finally {
            root.deleteRecursively()
        }
    }
}
