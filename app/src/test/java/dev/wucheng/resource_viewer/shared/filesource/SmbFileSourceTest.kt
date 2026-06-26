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
            FileEntry("file1.jpg", "/share/file1.jpg", false, 1024, 1000, "jpg"),
            FileEntry("subfolder", "/share/subfolder", true, 0, 2000, "")
        )
        every { mockWrapper.listDirectory("/share") } returns entries

        // When
        val result = smbFileSource.listDirectory("")

        // Then
        assertEquals(2, result.size)
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "share") }
        verify { mockWrapper.listDirectory("/share") }
    }

    @Test
    fun `listDirectory should handle relative path correctly`() = runTest {
        // Given
        val entries = listOf(
            FileEntry("image.png", "/share/folder/image.png", false, 500, 3000, "png")
        )
        every { mockWrapper.listDirectory("/share/folder") } returns entries

        // When
        val result = smbFileSource.listDirectory("folder")

        // Then
        assertEquals(1, result.size)
        verify { mockWrapper.listDirectory("/share/folder") }
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
        val entry = FileEntry("test.txt", "/share/test.txt", false, 100, 5000, "txt")
        every { mockWrapper.stat("/share/test.txt") } returns entry

        // When
        val result = smbFileSource.stat("test.txt")

        // Then
        assertNotNull(result)
        assertEquals("test.txt", result?.name)
    }

    @Test
    fun `stat should return null when file not found`() = runTest {
        // Given
        every { mockWrapper.stat("/share/nonexistent.txt") } returns null

        // When
        val result = smbFileSource.stat("nonexistent.txt")

        // Then
        assertNull(result)
    }

    @Test
    fun `readFile should return file content`() = runTest {
        // Given
        val content = "Hello, SMB!".toByteArray()
        every { mockWrapper.readFile("/share/test.txt") } returns content

        // When
        val result = smbFileSource.readFile("test.txt")

        // Then
        assertArrayEquals(content, result)
    }

    @Test
    fun `readRange should return specified range`() = runTest {
        // Given
        val rangeContent = "World".toByteArray()
        every { mockWrapper.readRange("/share/test.txt", 7L, 5L) } returns rangeContent

        // When
        val result = smbFileSource.readRange("test.txt", 7L, 5L)

        // Then
        assertArrayEquals(rangeContent, result)
    }

    @Test
    fun `openInputStream should return InputStream`() {
        // Given
        val content = "Stream content".toByteArray()
        every { mockWrapper.openInputStream("/share/test.txt") } returns ByteArrayInputStream(content)

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
        every { mockWrapper.listDirectory("/myshare/folder") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "myshare") }
        verify { mockWrapper.listDirectory("/myshare/folder") }
    }

    @Test
    fun `should handle rootPath with only share name`() = runTest {
        // Given
        val sourceWithShareOnly = testSource.copy(rootPath = "/myshare")
        val fileSource = SmbFileSource(sourceWithShareOnly, testPassword, mockWrapper)
        val entries = emptyList<FileEntry>()
        every { mockWrapper.listDirectory("/myshare") } returns entries

        // When
        fileSource.listDirectory("")

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "myshare") }
        verify { mockWrapper.listDirectory("/myshare") }
    }
}
