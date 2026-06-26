package dev.wucheng.resource_viewer.data.remote.smb

import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.EnumSet

class SmbClientWrapperTest {

    private lateinit var mockClient: SMBClient
    private lateinit var mockConnection: Connection
    private lateinit var mockSession: Session
    private lateinit var mockShare: DiskShare
    private lateinit var wrapper: SmbClientWrapper

    private val testHost = "192.168.1.100"
    private val testPort = 445
    private val testUsername = "testuser"
    private val testPassword = "testpass"
    private val testDomain = "WORKGROUP"
    private val testShareName = "testshare"

    @Before
    fun setup() {
        mockClient = mockk(relaxed = true)
        mockConnection = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        mockShare = mockk(relaxed = true)

        wrapper = SmbClientWrapper(mockClient)
    }

    @Test
    fun `connect should establish connection and authenticate`() {
        // Given
        every { mockClient.connect(testHost, testPort) } returns mockConnection
        every { mockConnection.authenticate(any<AuthenticationContext>()) } returns mockSession
        every { mockSession.connectShare(testShareName) } returns mockShare

        // When
        wrapper.connect(testHost, testPort, testUsername, testPassword, testDomain, testShareName)

        // Then
        verify { mockClient.connect(testHost, testPort) }
        verify { mockConnection.authenticate(any()) }
        verify { mockSession.connectShare(testShareName) }
        assertTrue(wrapper.isConnected())
    }

    @Test(expected = SmbConnectionException::class)
    fun `connect should throw SmbConnectionException when connection fails`() {
        // Given
        every { mockClient.connect(testHost, testPort) } throws RuntimeException("Connection refused")

        // When / Then
        wrapper.connect(testHost, testPort, testUsername, testPassword, testDomain, testShareName)
    }

    @Test(expected = SmbAuthException::class)
    fun `connect should throw SmbAuthException when authentication fails`() {
        // Given
        every { mockClient.connect(testHost, testPort) } returns mockConnection
        every { mockConnection.authenticate(any<AuthenticationContext>()) } throws RuntimeException("Auth failed")

        // When / Then
        wrapper.connect(testHost, testPort, testUsername, testPassword, testDomain, testShareName)
    }

    @Test
    fun `listDirectory should return file entries`() {
        // Given
        connectWrapper()
        val mockFile1 = mockk<FileIdBothDirectoryInformation>()
        every { mockFile1.fileName } returns "file1.jpg"
        every { mockFile1.endOfFile } returns 1024L
        every { mockFile1.lastWriteTime } returns FileTime(1000L)
        every { mockFile1.fileAttributes } returns 0x20 // FILE_ATTRIBUTE_ARCHIVE

        val mockDir = mockk<FileIdBothDirectoryInformation>()
        every { mockDir.fileName } returns "subfolder"
        every { mockDir.endOfFile } returns 0L
        every { mockDir.lastWriteTime } returns FileTime(2000L)
        every { mockDir.fileAttributes } returns 0x10 // FILE_ATTRIBUTE_DIRECTORY

        every { mockShare.list("/") } returns listOf(mockFile1, mockDir)

        // When
        val entries = wrapper.listDirectory("/")

        // Then
        assertEquals(2, entries.size)
        assertEquals("file1.jpg", entries[0].name)
        assertFalse(entries[0].isDirectory)
        assertEquals(1024L, entries[0].size)
        assertEquals("subfolder", entries[1].name)
        assertTrue(entries[1].isDirectory)
    }

    @Test
    fun `listDirectory should filter out dot entries`() {
        // Given
        connectWrapper()
        val dotEntry = mockk<FileIdBothDirectoryInformation>()
        every { dotEntry.fileName } returns "."
        every { dotEntry.endOfFile } returns 0L
        every { dotEntry.lastWriteTime } returns FileTime(0L)
        every { dotEntry.fileAttributes } returns 0x10

        val dotDotEntry = mockk<FileIdBothDirectoryInformation>()
        every { dotDotEntry.fileName } returns ".."
        every { dotDotEntry.endOfFile } returns 0L
        every { dotDotEntry.lastWriteTime } returns FileTime(0L)
        every { dotDotEntry.fileAttributes } returns 0x10

        val normalFile = mockk<FileIdBothDirectoryInformation>()
        every { normalFile.fileName } returns "image.png"
        every { normalFile.endOfFile } returns 500L
        every { normalFile.lastWriteTime } returns FileTime(3000L)
        every { normalFile.fileAttributes } returns 0x20

        every { mockShare.list("/") } returns listOf(dotEntry, dotDotEntry, normalFile)

        // When
        val entries = wrapper.listDirectory("/")

        // Then
        assertEquals(1, entries.size)
        assertEquals("image.png", entries[0].name)
    }

    @Test
    fun `stat should return file entry when file exists`() {
        // Given
        connectWrapper()
        val mockFile = mockk<FileIdBothDirectoryInformation>()
        every { mockFile.fileName } returns "test.txt"
        every { mockFile.endOfFile } returns 100L
        every { mockFile.lastWriteTime } returns FileTime(5000L)
        every { mockFile.fileAttributes } returns 0x20

        every { mockShare.list("/path") } returns listOf(mockFile)

        // When
        val entry = wrapper.stat("/path/test.txt")

        // Then
        assertNotNull(entry)
        assertEquals("test.txt", entry?.name)
        assertEquals(100L, entry?.size)
    }

    @Test
    fun `stat should return null when file not found`() {
        // Given
        connectWrapper()
        every { mockShare.list("/path") } returns emptyList()

        // When
        val entry = wrapper.stat("/path/nonexistent.txt")

        // Then
        assertNull(entry)
    }

    @Test
    fun `readFile should return file content as ByteArray`() {
        // Given
        connectWrapper()
        val mockFile = mockk<File>(relaxed = true)
        val content = "Hello, SMB!".toByteArray()

        every {
            mockShare.openFile(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockFile
        every { mockFile.inputStream } returns ByteArrayInputStream(content)

        // When
        val result = wrapper.readFile("/test.txt")

        // Then
        assertArrayEquals(content, result)
    }

    @Test
    fun `openInputStream should return InputStream`() {
        // Given
        connectWrapper()
        val mockFile = mockk<File>(relaxed = true)
        val content = "Stream content".toByteArray()

        every {
            mockShare.openFile(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockFile
        every { mockFile.inputStream } returns ByteArrayInputStream(content)

        // When
        val stream: InputStream = wrapper.openInputStream("/stream.txt")

        // Then
        assertNotNull(stream)
        val readContent = stream.readBytes()
        assertArrayEquals(content, readContent)
    }

    @Test
    fun `testConnection should succeed when connection works`() {
        // Given
        every { mockClient.connect(testHost, testPort) } returns mockConnection
        every { mockConnection.authenticate(any<AuthenticationContext>()) } returns mockSession
        every { mockSession.connectShare(testShareName) } returns mockShare

        // When / Then — 不抛异常即成功
        wrapper.testConnection(testHost, testPort, testUsername, testPassword, testDomain, testShareName)
    }

    @Test(expected = SmbConnectionException::class)
    fun `testConnection should throw when connection fails`() {
        // Given
        every { mockClient.connect(testHost, testPort) } throws RuntimeException("Connection failed")

        // When / Then
        wrapper.testConnection(testHost, testPort, testUsername, testPassword, testDomain, testShareName)
    }

    @Test
    fun `disconnect should close connection`() {
        // Given
        connectWrapper()

        // When
        wrapper.disconnect()

        // Then
        assertFalse(wrapper.isConnected())
        verify { mockShare.close() }
        verify { mockSession.close() }
        verify { mockConnection.close() }
    }

    @Test
    fun `readRange should read specified range of file`() {
        // Given
        connectWrapper()
        val mockFile = mockk<File>(relaxed = true)
        val rangeContent = "World".toByteArray()

        every {
            mockShare.openFile(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockFile
        every { mockFile.read(any(), any(), any(), any()) } answers {
            val dest = firstArg<ByteArray>()
            val length = rangeContent.size
            System.arraycopy(rangeContent, 0, dest, 0, length)
            length
        }

        // When
        val result = wrapper.readRange("/test.txt", 7L, 5L)

        // Then
        assertArrayEquals(rangeContent, result)
    }

    private fun connectWrapper() {
        every { mockClient.connect(testHost, testPort) } returns mockConnection
        every { mockConnection.authenticate(any<AuthenticationContext>()) } returns mockSession
        every { mockSession.connectShare(testShareName) } returns mockShare
        wrapper.connect(testHost, testPort, testUsername, testPassword, testDomain, testShareName)
    }
}
