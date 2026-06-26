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
    fun `createBackup should not close database singleton`() = runTest {
        val backupDir = File(tempDir, "backups")

        backupManager.createBackup(backupDir)

        verify(exactly = 0) { database.close() }
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

    @Test
    fun `deleteBackup should succeed when backup file does not exist`() = runTest {
        val backupFile = File(tempDir, "non_existent_backup.db")

        val result = backupManager.deleteBackup(backupFile)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `restoreFromBackup should fail when backup file does not exist`() = runTest {
        val backupFile = File(tempDir, "non_existent_backup.db")

        val result = backupManager.restoreFromBackup(backupFile)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BackupFileNotFoundException)
    }

    @Test
    fun `restoreFromBackup should succeed with valid backup file`() = runTest {
        // Create a backup first
        val backupDir = File(tempDir, "backups")
        val backupResult = backupManager.createBackup(backupDir)
        assertTrue(backupResult.isSuccess)
        val backupFile = backupResult.getOrNull()!!

        // Now restore from it
        val restoreResult = backupManager.restoreFromBackup(backupFile)
        assertTrue(restoreResult.isSuccess)
    }

    @Test
    fun `getBackupFiles should return empty list for non-existent directory`() {
        val nonExistentDir = File(tempDir, "non_existent_dir")

        val files = backupManager.getBackupFiles(nonExistentDir)

        assertTrue(files.isEmpty())
    }

    @Test
    fun `getBackupFiles should ignore non-backup files`() {
        val backupDir = File(tempDir, "backups")
        backupDir.mkdirs()

        // Create test backup files
        val backupFile = File(backupDir, "backup_20260101_120000.db")
        val nonBackupFile = File(backupDir, "other_file.db")
        val txtFile = File(backupDir, "backup_test.txt")

        backupFile.createNewFile()
        nonBackupFile.createNewFile()
        txtFile.createNewFile()

        val files = backupManager.getBackupFiles(backupDir)

        assertEquals(1, files.size)
        assertEquals(backupFile.name, files[0].file.name)
    }

    @Test
    fun `createBackup should create backup directory if it does not exist`() = runTest {
        val backupDir = File(tempDir, "new_backup_dir")

        val result = backupManager.createBackup(backupDir)

        assertTrue(result.isSuccess)
        assertTrue(backupDir.exists())
    }
}
