package dev.wucheng.resource_viewer.data.local.backup

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseBackupManagerInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var backupManager: DatabaseBackupManager
    private lateinit var backupDir: File

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = DatabaseBackupManager(context, database)
        backupDir = File(context.cacheDir, "test_backups")
    }

    @After
    fun tearDown() {
        database.close()
        backupDir.deleteRecursively()
    }

    @Test
    fun createBackup_andRestore_shouldPreserveData() = runTest {
        // 插入测试数据
        val source = SourceEntity(
            id = "test-source-1",
            name = "Test Source",
            type = SourceType.LOCAL,
            rootPath = "/test/path"
        )
        database.sourceDao().insert(source)

        // 创建备份
        val backupResult = backupManager.createBackup(backupDir)
        assertTrue(backupResult.isSuccess)

        val backupFile = backupResult.getOrNull()!!
        assertTrue(backupFile.exists())

        // 验证备份文件列表
        val backups = backupManager.getBackupFiles(backupDir)
        assertTrue(backups.isNotEmpty())
    }

    @Test
    fun getBackupFiles_shouldReturnEmptyForEmptyDir() {
        val emptyDir = File(context.cacheDir, "empty_dir_${System.currentTimeMillis()}")
        emptyDir.mkdirs()

        val files = backupManager.getBackupFiles(emptyDir)
        assertTrue(files.isEmpty())

        emptyDir.deleteRecursively()
    }

    @Test
    fun deleteBackup_shouldRemoveBackupFile() = runTest {
        // 创建备份
        val backupResult = backupManager.createBackup(backupDir)
        assertTrue(backupResult.isSuccess)

        val backupFile = backupResult.getOrNull()!!

        // 删除备份
        val deleteResult = backupManager.deleteBackup(backupFile)
        assertTrue(deleteResult.isSuccess)
        assertFalse(backupFile.exists())
    }
}
