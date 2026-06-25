package dev.wucheng.resource_viewer.data.local.backup

import android.content.Context
import dev.wucheng.resource_viewer.data.local.AppDatabase
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class DatabaseBackupManagerTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var backupManager: DatabaseBackupManager
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        database = mockk(relaxed = true)
        tempDir = File(System.getProperty("java.io.tmpdir"), "backup_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        // Mock database path
        val dbFile = File(tempDir, "test.db")
        dbFile.createNewFile()

        every { context.getDatabasePath(any()) } returns dbFile

        backupManager = DatabaseBackupManager(context, database)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `createBackup should create backup file`() = runTest {
        val backupDir = File(tempDir, "backups")

        val result = backupManager.createBackup(backupDir)

        assertTrue(result.isSuccess)
        val backupFile = result.getOrNull()
        assertNotNull(backupFile)
        assertTrue(backupFile!!.exists())
        assertTrue(backupFile.name.startsWith("backup_"))
        assertTrue(backupFile.name.endsWith(".db"))
    }

    @Test
    fun `createBackup should close database before backup`() = runTest {
        val backupDir = File(tempDir, "backups")

        backupManager.createBackup(backupDir)

        verify { database.close() }
    }

    @Test
    fun `getBackupFiles should return empty list for empty directory`() {
        val emptyDir = File(tempDir, "empty")
        emptyDir.mkdirs()

        val files = backupManager.getBackupFiles(emptyDir)

        assertTrue(files.isEmpty())
    }

    @Test
    fun `getBackupFiles should return backup files sorted by date`() {
        val backupDir = File(tempDir, "backups")
        backupDir.mkdirs()

        // Create test backup files
        val file1 = File(backupDir, "backup_20260101_120000.db")
        val file2 = File(backupDir, "backup_20260102_120000.db")
        file1.createNewFile()
        file2.createNewFile()

        // Set different last modified times
        file1.setLastModified(System.currentTimeMillis() - 100000)
        file2.setLastModified(System.currentTimeMillis())

        val files = backupManager.getBackupFiles(backupDir)

        assertEquals(2, files.size)
        // Should be sorted by date descending
        assertTrue(files[0].createdAt >= files[1].createdAt)
    }

    @Test
    fun `deleteBackup should delete backup file and associated files`() = runTest {
        val backupDir = File(tempDir, "backups")
        backupDir.mkdirs()

        val backupFile = File(backupDir, "backup_test.db")
        val walFile = File(backupDir, "backup_test.db-wal")
        val shmFile = File(backupDir, "backup_test.db-shm")

        backupFile.createNewFile()
        walFile.createNewFile()
        shmFile.createNewFile()

        val result = backupManager.deleteBackup(backupFile)

        assertTrue(result.isSuccess)
        assertFalse(backupFile.exists())
        assertFalse(walFile.exists())
        assertFalse(shmFile.exists())
    }
}
