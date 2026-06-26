package dev.wucheng.resource_viewer.data.remote.smb

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.domain.model.Source
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SmbDataSourceFactory 测试。
 */
class SmbDataSourceFactoryTest {

    private lateinit var factory: SmbDataSourceFactory

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
        factory = SmbDataSourceFactory(testSource, testPassword)
    }

    @Test
    fun `createDataSource should return SmbDataSource`() {
        // When
        val dataSource = factory.createDataSource()

        // Then
        assertNotNull(dataSource)
        assertTrue(dataSource is SmbDataSource)
    }

    @Test
    fun `createDataSource should return new instance each time`() {
        // When
        val dataSource1 = factory.createDataSource()
        val dataSource2 = factory.createDataSource()

        // Then
        assertNotSame(dataSource1, dataSource2)
    }
}
