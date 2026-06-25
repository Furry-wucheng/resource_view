package dev.wucheng.resource_viewer.data.local.backup

import android.content.Context
import android.net.Uri
import dev.wucheng.resource_viewer.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据库备份管理器
 * 提供数据库备份和恢复功能
 */
class DatabaseBackupManager(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val dbFile: File
        get() = context.getDatabasePath(AppDatabase.DATABASE_NAME)

    private val walFile: File
        get() = File(dbFile.path + "-wal")

    private val shmFile: File
        get() = File(dbFile.path + "-shm")

    /**
     * 创建数据库备份
     * @param backupDir 备份文件保存目录
     * @return 备份文件路径，失败返回 null
     */
    suspend fun createBackup(backupDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 确保数据库连接关闭，以便安全复制
            database.close()

            // 确保备份目录存在
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // 生成备份文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "backup_${timestamp}.db")

            // 复制数据库文件
            dbFile.copyTo(backupFile, overwrite = true)

            // 复制 WAL 和 SHM 文件（如果存在）
            if (walFile.exists()) {
                walFile.copyTo(File(backupFile.path + "-wal"), overwrite = true)
            }
            if (shmFile.exists()) {
                shmFile.copyTo(File(backupFile.path + "-shm"), overwrite = true)
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从备份文件恢复数据库
     * @param backupFile 备份文件路径
     * @return 恢复结果
     */
    suspend fun restoreFromBackup(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext Result.failure(
                    BackupFileNotFoundException("备份文件不存在: ${backupFile.path}")
                )
            }

            // 关闭数据库连接
            database.close()

            // 清理现有 WAL 和 SHM 文件
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 复制备份文件到数据库位置
            backupFile.copyTo(dbFile, overwrite = true)

            // 恢复 WAL 和 SHM 文件（如果存在）
            val backupWal = File(backupFile.path + "-wal")
            val backupShm = File(backupFile.path + "-shm")

            if (backupWal.exists()) {
                backupWal.copyTo(walFile, overwrite = true)
            }
            if (backupShm.exists()) {
                backupShm.copyTo(shmFile, overwrite = true)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从 URI 恢复数据库
     * @param uri 备份文件的 URI
     * @return 恢复结果
     */
    suspend fun restoreFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 关闭数据库连接
            database.close()

            // 清理现有文件
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 从 URI 复制数据
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(
                BackupFileNotFoundException("无法打开 URI: $uri")
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取备份文件列表
     * @param backupDir 备份目录
     * @return 备份文件列表
     */
    fun getBackupFiles(backupDir: File): List<BackupInfo> {
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles()
            ?.filter { it.name.startsWith("backup_") && it.name.endsWith(".db") }
            ?.map { file ->
                BackupInfo(
                    file = file,
                    size = file.length(),
                    createdAt = file.lastModified(),
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    /**
     * 删除指定备份文件
     * @param backupFile 备份文件
     * @return 删除结果
     */
    suspend fun deleteBackup(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 删除主备份文件
            if (backupFile.exists()) {
                backupFile.delete()
            }

            // 删除关联的 WAL 和 SHM 文件
            val wal = File(backupFile.path + "-wal")
            val shm = File(backupFile.path + "-shm")
            if (wal.exists()) wal.delete()
            if (shm.exists()) shm.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 备份文件信息
 */
data class BackupInfo(
    val file: File,
    val size: Long,
    val createdAt: Long,
)

/**
 * 备份文件不存在异常
 */
class BackupFileNotFoundException(message: String) : Exception(message)
