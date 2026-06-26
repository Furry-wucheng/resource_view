package dev.wucheng.resource_viewer.data.remote.smb

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * SmbDataSource 测试。
 * 使用 MockK 进行单元测试。
 *
 * 注意：由于 android.net.Uri 在单元测试中不可用，需要 mock Uri。
 */
class SmbDataSourceTest {

    private lateinit var mockWrapper: SmbClientWrapper
    private lateinit var smbDataSource: SmbDataSource
    private lateinit var mockUri: Uri

    private val testSource = Source(
        id = "test-smb-id",
        name = "Test SMB",
        type = SourceType.SMB,
        rootPath = "/share/videos",
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
        mockUri = mockk(relaxed = true)
        every { mockUri.path } returns "test.mp4"
        smbDataSource = SmbDataSource(testSource, testPassword, mockWrapper)
    }

    @Test
    fun `open should connect and return bytes remaining`() {
        // Given
        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()
        val inputStream = ByteArrayInputStream("test content".toByteArray())
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/test.mp4") } returns inputStream
        every { mockWrapper.stat("videos/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/test.mp4",
            isDirectory = false,
            size = 12,
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        // When
        val bytesRemaining = smbDataSource.open(dataSpec)

        // Then
        assertEquals(12L, bytesRemaining)
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "share") }
        verify { mockWrapper.openInputStream("videos/test.mp4") }
    }

    @Test
    fun `open should skip to position`() {
        // Given
        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .setPosition(100)
            .build()
        val inputStream = ByteArrayInputStream(ByteArray(200))
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/test.mp4") } returns inputStream
        every { mockWrapper.stat("videos/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/test.mp4",
            isDirectory = false,
            size = 300,
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        // When
        val bytesRemaining = smbDataSource.open(dataSpec)

        // Then
        assertEquals(200L, bytesRemaining)
    }

    @Test
    fun `open should throw when connection fails`() {
        // Given
        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.connect(any(), any(), any(), any(), any(), any()) } throws SmbConnectionException("Connection failed")

        // When / Then
        try {
            smbDataSource.open(dataSpec)
            fail("Should throw SmbConnectionException")
        } catch (e: SmbConnectionException) {
            assertEquals("Connection failed", e.message)
        }
    }

    @Test
    fun `read should return bytes read`() {
        // Given
        val content = "Hello, SMB!".toByteArray()
        val inputStream = ByteArrayInputStream(content)
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/test.mp4") } returns inputStream
        every { mockWrapper.stat("videos/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/test.mp4",
            isDirectory = false,
            size = content.size.toLong(),
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()
        smbDataSource.open(dataSpec)

        // When
        val buffer = ByteArray(100)
        val bytesRead = smbDataSource.read(buffer, 0, 100)

        // Then
        assertEquals(content.size, bytesRead)
        assertArrayEquals(content, buffer.copyOf(bytesRead))
    }

    @Test
    fun `read should return end of input when no more data`() {
        // Given
        val inputStream = ByteArrayInputStream(ByteArray(0))
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/test.mp4") } returns inputStream
        every { mockWrapper.stat("videos/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/test.mp4",
            isDirectory = false,
            size = 0,
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()
        smbDataSource.open(dataSpec)

        // When
        val buffer = ByteArray(100)
        val bytesRead = smbDataSource.read(buffer, 0, 100)

        // Then
        assertEquals(C.RESULT_END_OF_INPUT, bytesRead)
    }

    @Test
    fun `close should close input stream`() {
        // Given
        val mockInputStream = mockk<InputStream>(relaxed = true)
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/test.mp4") } returns mockInputStream
        every { mockWrapper.stat("videos/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/test.mp4",
            isDirectory = false,
            size = 100,
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()
        smbDataSource.open(dataSpec)

        // When
        smbDataSource.close()

        // Then
        verify { mockInputStream.close() }
    }

    @Test
    fun `getUri should return null`() {
        // When
        val uri = smbDataSource.getUri()

        // Then
        assertNull(uri)
    }

    @Test
    fun `should handle source with null host`() {
        // Given
        val sourceWithoutHost = testSource.copy(host = null)
        val dataSource = SmbDataSource(sourceWithoutHost, testPassword, mockWrapper)
        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()
        every { mockWrapper.isConnected() } returns false

        // When / Then
        try {
            dataSource.open(dataSpec)
            fail("Should throw SmbConnectionException")
        } catch (e: SmbConnectionException) {
            assertEquals("Host not specified", e.message)
        }
    }

    @Test
    fun `should handle source with default port`() {
        // Given
        val sourceWithDefaultPort = testSource.copy(port = null)
        val dataSource = SmbDataSource(sourceWithDefaultPort, testPassword, mockWrapper)
        val inputStream = ByteArrayInputStream("test".toByteArray())
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/test.mp4") } returns inputStream
        every { mockWrapper.stat("videos/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/test.mp4",
            isDirectory = false,
            size = 4,
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()

        // When
        dataSource.open(dataSpec)

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "share") }
    }

    @Test
    fun `should parse share name from rootPath correctly`() {
        // Given
        val sourceWithDeepPath = testSource.copy(rootPath = "/myshare/videos/movies")
        val dataSource = SmbDataSource(sourceWithDeepPath, testPassword, mockWrapper)
        val inputStream = ByteArrayInputStream("test".toByteArray())
        every { mockWrapper.isConnected() } returns false
        every { mockWrapper.openInputStream("videos/movies/test.mp4") } returns inputStream
        every { mockWrapper.stat("videos/movies/test.mp4") } returns FileEntry(
            name = "test.mp4",
            relativePath = "videos/movies/test.mp4",
            isDirectory = false,
            size = 4,
            modifiedAt = System.currentTimeMillis(),
            extension = "mp4"
        )

        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .build()

        // When
        dataSource.open(dataSpec)

        // Then
        verify { mockWrapper.connect("192.168.1.100", 445, "testuser", "testpass", "WORKGROUP", "myshare") }
    }
}
