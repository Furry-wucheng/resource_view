package dev.wucheng.resource_viewer.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Source
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * FilesystemRepository 测试。
 * 使用 Room 内存数据库和 MockK 进行测试。
 */
@RunWith(AndroidJUnit4::class)
class FilesystemRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FilesystemRepository
    private lateinit var mockSecurePrefs: SecurePrefs

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        mockSecurePrefs = mockk()
        repo = FilesystemRepository(db.sourceDao(), mockSecurePrefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `should return error when listing directory for SMB source without password`() = runTest {
        val sourceId = UUID.randomUUID().toString()
        db.sourceDao().insert(SourceEntity(
            id = sourceId,
            name = "SMB Source",
            type = SourceType.SMB,
            rootPath = "smb://192.168.1.100/share",
        ))
        every { mockSecurePrefs.getPassword(sourceId) } returns null

        val source = Source(
            id = sourceId,
            name = "SMB Source",
            type = SourceType.SMB,
            rootPath = "smb://192.168.1.100/share",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val result = repo.listDirectory(source, "/")
        assertTrue(result is Result.Err)
    }

    @Test
    fun `should return error when source not found for getFileSource`() = runTest {
        val result = repo.getFileSource("non-existent-id")
        assertTrue(result is Result.Err)
    }

    @Test
    fun `should return error when testing connection for unreachable source`() = runTest {
        val sourceId = UUID.randomUUID().toString()
        db.sourceDao().insert(SourceEntity(
            id = sourceId,
            name = "Test Source",
            type = SourceType.LOCAL,
            rootPath = "/non/existent/path",
        ))
        every { mockSecurePrefs.getPassword(sourceId) } returns null

        val source = Source(
            id = sourceId,
            name = "Test Source",
            type = SourceType.LOCAL,
            rootPath = "/non/existent/path",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        // Note: This test will throw NotImplementedError because LocalFileSource is a placeholder
        // In a real implementation, this would test actual connection
        val result = repo.testConnection(source)
        assertTrue(result is Result.Err)
    }
}
