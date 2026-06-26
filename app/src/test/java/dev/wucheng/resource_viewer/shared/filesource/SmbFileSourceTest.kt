package dev.wucheng.resource_viewer.shared.filesource

import dev.wucheng.resource_viewer.data.remote.smb.SmbClientWrapper
import dev.wucheng.resource_viewer.data.remote.smb.SmbConnectionException
import dev.wucheng.resource_viewer.data.remote.smb.SmbAuthException
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class SmbFileSourceTest {

    private lateinit var mockWrapper: SmbClientWrapper
    private lateinit var smbFileSource: SmbFileSource

    private val testSource = Source(
        id = "test-smb-id",
        name = "Test SMB",
        type = SourceType.SMB,
        rootPath = "/share",
        host = "192.168.1.100",
        port = 445,
        username = "testuser",
        domain = "WORKGROUP",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    private val testPassword = "testpass"

    @Before
    fun setup() {
        mockWrapper = mockk(relaxed = true)
        smbFileSource = SmbFileSource(testSource, testPassword, mockWrapper)
    }

    @Test
    fun `sourceId should return source id`() {
        // Then
        assertEquals("test-smb-id", smbFileSource.sourceId)
    }

    @Test
    fun `listDirectory should connect and return entries`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("file1.jpg", "file1.jpg", false, 1024, 1000, "jpg"),
            FileEntry("subfolder", "subfolder", true, 0, 2000, "")
        )
        every { mockWrapper.listDirectory("") } returns entries

        // When
        val result = smbFileSource.listDirectory("")

        // Then
        assertEquals(2, result.size)
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "share") }
        verify { mockWrapper.listDirectory("") }
    }

    @Test
    fun `listDirectory should handle relative path correctly`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("image.png", "folder/image.png", false, 500, 3000, "png")
        )
        every { mockWrapper.listDirectory("folder") } returns entries

        // When
        val result = smbFileSource.listDirectory("folder")

        // Then
        assertEquals(1, result.size)
        verify { mockWrapper.listDirectory("folder") }
    }

    @Test(expected = SmbConnectionException::class)
    fun `listDirectory should throw SmbConnectionException when connection fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbConnectionException("Connection failed")

        // When / Then
        smbFileSource.listDirectory("")
    }

    @Test(expected = SmbAuthException::class)
    fun `listDirectory should throw SmbAuthException when authentication fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbAuthException("Auth failed")

        // When / Then
        smbFileSource.listDirectory("")
    }

    @Test
    fun `stat should return file entry`() = runTest {
        // Given
        val entry = FileEntry("test.txt", "test.txt", false, 100, 5000, "txt")
        every { mockWrapper.stat("test.txt") } returns entry

        // When
        val result = smbFileSource.stat("test.txt")

        // Then
        assertNotNull(result)
        assertEquals("test.txt", result?.name)
    }

    @Test
    fun `stat should return null when file not found`() = runTest {
        // Given
        every { mockWrapper.stat("nonexistent.txt") } returns null

        // When
        val result = smbFileSource.stat("nonexistent.txt")

        // Then
        assertNull(result)
    }

    @Test
    fun `readFile should return file content`() = runTest {
        // Given
        val content = "Hello, SMB!".toByteArray()
        every { mockWrapper.readFile("test.txt") } returns content

        // When
        val result = smbFileSource.readFile("test.txt")

        // Then
        assertArrayEquals(content, result)
    }

    @Test
    fun `readRange should return specified range`() = runTest {
        // Given
        val rangeContent = "World".toByteArray()
        every { mockWrapper.readRange("test.txt", 7L, 5L) } returns rangeContent

        // When
        val result = smbFileSource.readRange("test.txt", 7L, 5L)

        // Then
        assertArrayEquals(rangeContent, result)
    }

    @Test
    fun `openInputStream should return InputStream`() {
        // Given
        val content = "Stream content".toByteArray()
        every { mockWrapper.openInputStream("test.txt") } returns ByteArrayInputStream(content)

        // When
        val stream = smbFileSource.openInputStream("test.txt")

        // Then
        assertNotNull(stream)
        assertArrayEquals(content, stream.readBytes())
    }

    @Test
    fun `testConnection should return true when connection succeeds`() = runTest {
        // Given
        every {
            mockWrapper.testConnection(any(), any(), any(), any(), any(), any())
        } returns true

        // When
        val result = smbFileSource.testConnection()

        // Then
        assertTrue(result)
    }

    @Test
    fun `testConnection should return false when connection fails`() = runTest {
        // Given
        every {
            mockWrapper.testConnection(any(), any(), any(), any(), any(), any())
        } returns false

        // When
        val result = smbFileSource.testConnection()

        // Then
        assertFalse(result)
    }

    @Test
    fun `disconnect should call wrapper disconnect`() {
        // When
        smbFileSource.disconnect()

        // Then
        verify { mockWrapper.disconnect() }
    }

    @Test
    fun `should parse share name from rootPath correctly`() = runTest {
        // Given
        val sourceWithSlashes = testSource.copy(rootPath = "/myshare/folder")
        val fileSource = SmbFileSource(sourceWithSlashes, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("folder") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "myshare") }
        verify { mockWrapper.listDirectory("folder") }
    }

    @Test
    fun `should handle rootPath with only share name`() = runTest {
        // Given
        val sourceWithShareOnly = testSource.copy(rootPath = "/myshare")
        val fileSource = SmbFileSource(sourceWithShareOnly, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "myshare") }
        verify { mockWrapper.listDirectory("") }
    }

    @Test
    fun `should handle rootPath with multiple path segments`() = runTest {
        // Given
        val sourceWithDeepPath = testSource.copy(rootPath = "/share/folder/subfolder")
        val fileSource = SmbFileSource(sourceWithDeepPath, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("folder/subfolder") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "share") }
        verify { mockWrapper.listDirectory("folder/subfolder") }
    }

    @Test
    fun `should handle relative path with multiple segments`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("file.txt", "folder/sub/file.txt", false, 100, 1000, "txt")
        )
        every { mockWrapper.listDirectory("folder/sub") } returns entries

        // When
        val result = smbFileSource.listDirectory("folder/sub")

        // Then
        assertEquals(1, result.size)
        verify { mockWrapper.listDirectory("folder/sub") }
    }

    @Test(expected = SmbConnectionException::class)
    fun `stat should throw SmbConnectionException when connection fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbConnectionException("Connection failed")

        // When / Then
        smbFileSource.stat("test.txt")
    }

    @Test(expected = SmbAuthException::class)
    fun `stat should throw SmbAuthException when authentication fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbAuthException("Auth failed")

        // When / Then
        smbFileSource.stat("test.txt")
    }

    @Test(expected = SmbConnectionException::class)
    fun `readFile should throw SmbConnectionException when connection fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbConnectionException("Connection failed")

        // When / Then
        smbFileSource.readFile("test.txt")
    }

    @Test(expected = SmbAuthException::class)
    fun `readFile should throw SmbAuthException when authentication fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbAuthException("Auth failed")

        // When / Then
        smbFileSource.readFile("test.txt")
    }

    @Test(expected = SmbConnectionException::class)
    fun `readRange should throw SmbConnectionException when connection fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbConnectionException("Connection failed")

        // When / Then
        smbFileSource.readRange("test.txt", 0L, 100L)
    }

    @Test(expected = SmbAuthException::class)
    fun `readRange should throw SmbAuthException when authentication fails`() = runTest {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbAuthException("Auth failed")

        // When / Then
        smbFileSource.readRange("test.txt", 0L, 100L)
    }

    @Test(expected = SmbConnectionException::class)
    fun `openInputStream should throw SmbConnectionException when connection fails`() {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbConnectionException("Connection failed")

        // When / Then
        smbFileSource.openInputStream("test.txt")
    }

    @Test(expected = SmbAuthException::class)
    fun `openInputStream should throw SmbAuthException when authentication fails`() {
        // Given
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbAuthException("Auth failed")

        // When / Then
        smbFileSource.openInputStream("test.txt")
    }

    @Test
    fun `testConnection should return false when host is null`() = runTest {
        // Given
        val sourceWithoutHost = testSource.copy(host = null)
        val fileSource = SmbFileSource(sourceWithoutHost, testPassword, mockWrapper)

        // When
        val result = fileSource.testConnection()

        // Then
        assertFalse(result)
    }

    @Test
    fun `testConnection should return false when exception occurs`() = runTest {
        // Given
        every {
            mockWrapper.testConnection(any(), any(), any(), any(), any(), any())
        } throws RuntimeException("Unexpected error")

        // When
        val result = smbFileSource.testConnection()

        // Then
        assertFalse(result)
    }

    @Test
    fun `should handle source with default port`() = runTest {
        // Given
        val sourceWithDefaultPort = testSource.copy(port = null)
        val fileSource = SmbFileSource(sourceWithDefaultPort, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "share") }
    }

    @Test
    fun `should handle source with custom port`() = runTest {
        // Given
        val sourceWithCustomPort = testSource.copy(port = 8445)
        val fileSource = SmbFileSource(sourceWithCustomPort, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 8445, "testuser", "testpass", "WORKGROUP", "share") }
    }

    @Test
    fun `should handle source with null username`() = runTest {
        // Given
        val sourceWithNullUsername = testSource.copy(username = null)
        val fileSource = SmbFileSource(sourceWithNullUsername, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "", "testpass", "WORKGROUP", "share") }
    }

    @Test
    fun `should handle source with null domain`() = runTest {
        // Given
        val sourceWithNullDomain = testSource.copy(domain = null)
        val fileSource = SmbFileSource(sourceWithNullDomain, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", null, "share") }
    }
}
