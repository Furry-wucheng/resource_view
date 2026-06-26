package dev.wucheng.resource_viewer.data.remote.smb

import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import dev.wucheng.resource_viewer.domain.model.FileEntry
import java.io.InputStream
import java.util.EnumSet

/**
 * SMB 客户端封装类。
 * 封装 smbj 的 SMBClient，提供简化的 SMB 文件操作接口。
 *
 * 注意：此实现遵循 doc/mvp/M17-smb-file-source.md 中的 M17.1 子任务。
 */
class SmbClientWrapper(private val client: SMBClient) {

    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null

    /**
     * 建立 SMB 连接并认证。
     * @param host 主机地址
     * @param port 端口号
     * @param username 用户名
     * @param password 密码
     * @param domain 域名（可选）
     * @param shareName 共享名称
     * @throws SmbConnectionException 连接失败时抛出
     * @throws SmbAuthException 认证失败时抛出
     */
    fun connect(
        host: String,
        port: Int,
        username: String,
        password: String,
        domain: String?,
        shareName: String
    ) {
        try {
            connection = client.connect(host, port)
        } catch (e: Exception) {
            Log.e(TAG, "SMB 连接失败: $host:$port", e)
            throw SmbConnectionException("Connection failed: ${e.message}", e)
        }

        try {
            val authContext = AuthenticationContext(
                username,
                password.toCharArray(),
                domain ?: ""
            )
            session = connection!!.authenticate(authContext)
        } catch (e: Exception) {
            Log.e(TAG, "SMB 认证失败: $host", e)
            connection?.close()
            connection = null
            throw SmbAuthException("Authentication failed: ${e.message}", e)
        }

        try {
            share = session!!.connectShare(shareName) as DiskShare
        } catch (e: Exception) {
            Log.e(TAG, "SMB 共享连接失败: $shareName", e)
            session?.close()
            connection?.close()
            session = null
            connection = null
            throw SmbConnectionException("Failed to connect share: ${e.message}", e)
        }
    }

    /**
     * 检查是否已连接。
     */
    fun isConnected(): Boolean {
        return share != null && session != null && connection != null
    }

    /**
     * 列出目录内容。
     * @param path 目录路径
     * @return 文件条目列表
     */
    fun listDirectory(path: String): List<FileEntry> {
        val currentShare = share ?: throw SmbConnectionException("Not connected")
        return try {
            val entries = currentShare.list(path)
            entries
                .filter { it.fileName != "." && it.fileName != ".." }
                .map { it.toFileEntry(path) }
        } catch (e: Exception) {
            throw SmbConnectionException("Failed to list directory: ${e.message}", e)
        }
    }

    /**
     * 获取文件/目录元数据。
     * @param fullPath 完整路径（包含文件名）
     * @return 文件条目，如果不存在则返回 null
     */
    fun stat(fullPath: String): FileEntry? {
        val currentShare = share ?: throw SmbConnectionException("Not connected")
        return try {
            val parentPath = fullPath.substringBeforeLast("/", missingDelimiterValue = "")
            val fileName = fullPath.substringAfterLast("/")
            val entries = currentShare.list(parentPath)
            entries
                .filter { it.fileName != "." && it.fileName != ".." }
                .find { it.fileName == fileName }
                ?.toFileEntry(parentPath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取完整文件内容。
     * @param path 文件路径
     * @return 文件内容字节数组
     */
    fun readFile(path: String): ByteArray {
        val currentShare = share ?: throw SmbConnectionException("Not connected")
        return try {
            val file = openFile(currentShare, path)
            file.use { it.inputStream.readBytes() }
        } catch (e: Exception) {
            throw SmbConnectionException("Failed to read file: ${e.message}", e)
        }
    }

    /**
     * 范围读取文件。
     * @param path 文件路径
     * @param offset 偏移量
     * @param length 长度
     * @return 指定范围的字节数组
     */
    fun readRange(path: String, offset: Long, length: Long): ByteArray {
        val currentShare = share ?: throw SmbConnectionException("Not connected")
        return try {
            val file = openFile(currentShare, path)
            file.use {
                val buffer = ByteArray(length.toInt())
                val bytesRead = it.read(buffer, offset, 0, length.toInt())
                if (bytesRead < length) {
                    buffer.copyOf(bytesRead.toInt())
                } else {
                    buffer
                }
            }
        } catch (e: Exception) {
            throw SmbConnectionException("Failed to read file range: ${e.message}", e)
        }
    }

    /**
     * 打开文件输入流。
     * @param path 文件路径
     * @return InputStream
     */
    fun openInputStream(path: String): InputStream {
        val currentShare = share ?: throw SmbConnectionException("Not connected")
        return try {
            val file = openFile(currentShare, path)
            file.inputStream
        } catch (e: Exception) {
            throw SmbConnectionException("Failed to open file stream: ${e.message}", e)
        }
    }

    /**
     * 测试连接。
     * @param host 主机地址
     * @param port 端口号
     * @param username 用户名
     * @param password 密码
     * @param domain 域名
     * @param shareName 共享名称
     * @throws SmbConnectionException 连接失败
     * @throws SmbAuthException 认证失败
     */
    fun testConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        domain: String?,
        shareName: String
    ) {
        var tempConnection: Connection? = null
        var tempSession: Session? = null
        var tempShare: DiskShare? = null
        try {
            tempConnection = client.connect(host, port)
            val authContext = AuthenticationContext(
                username,
                password.toCharArray(),
                domain ?: ""
            )
            tempSession = tempConnection.authenticate(authContext)
            tempShare = tempSession.connectShare(shareName) as DiskShare
        } catch (e: SmbConnectionException) {
            Log.e(TAG, "SMB 测试连接失败: $host:$port", e)
            throw e
        } catch (e: SmbAuthException) {
            Log.e(TAG, "SMB 测试认证失败: $host", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SMB 测试连接异常: $host:$port/$shareName", e)
            throw SmbConnectionException("连接失败: ${e.message}", e)
        } finally {
            try { tempShare?.close() } catch (_: Exception) {}
            try { tempSession?.close() } catch (_: Exception) {}
            try { tempConnection?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 断开连接并释放资源。
     */
    fun disconnect() {
        try {
            share?.close()
        } catch (_: Exception) {}
        try {
            session?.close()
        } catch (_: Exception) {}
        try {
            connection?.close()
        } catch (_: Exception) {}

        share = null
        session = null
        connection = null
    }

    private fun openFile(share: DiskShare, path: String): File {
        return share.openFile(
            path,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            null
        )
    }

    private fun FileIdBothDirectoryInformation.toFileEntry(parentPath: String): FileEntry {
        val isDir = (fileAttributes and 0x10) != 0L
        val relativePath = if (parentPath.isEmpty() || parentPath == "/") {
            fileName
        } else {
            "$parentPath/$fileName"
        }
        val extension = if (isDir) "" else fileName.substringAfterLast(".", "")

        return FileEntry(
            name = fileName,
            relativePath = relativePath,
            isDirectory = isDir,
            size = endOfFile,
            modifiedAt = lastWriteTime.toEpochMillis(),
            extension = extension
        )
    }

    private fun com.hierynomus.msdtyp.FileTime.toEpochMillis(): Long {
        // FileTime 转换为 epoch milliseconds
        return this.toDate().time
    }

    companion object {
        private const val TAG = "SmbClientWrapper"
    }
}
