package dev.wucheng.resource_viewer.shared.filesource

import dev.wucheng.resource_viewer.data.remote.smb.SmbClientWrapper
import dev.wucheng.resource_viewer.data.remote.smb.SmbAuthException
import dev.wucheng.resource_viewer.data.remote.smb.SmbConnectionException
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * SMB 文件源实现。
 * 实现 FileSource 接口，桥接 SmbClientWrapper。
 *
 * 注意：此实现遵循 doc/mvp/M17-smb-file-source.md 中的 M17.2 子任务。
 */
class SmbFileSource(
    private val source: Source,
    private val password: String,
    private val wrapper: SmbClientWrapper,
) : FileSource {

    override val sourceId: String = source.id

    /**
     * 从 rootPath 解析共享名称。
     * 例如："/myshare/folder" -> "myshare"
     */
    private val shareName: String
        get() {
            val path = source.rootPath.trimStart('/')
            return path.substringBefore('/')
        }

    /**
     * 获取共享内的基础路径。
     * 例如："/myshare/folder" -> "folder"
     */
    private val basePath: String
        get() {
            val path = source.rootPath.trimStart('/')
            return path.substringAfter('/', missingDelimiterValue = "").trim('/')
        }

    /**
     * 确保已连接到 SMB 服务器。
     */
    private fun ensureConnected() {
        if (!wrapper.isConnected()) {
            val host = source.host ?: throw SmbConnectionException("Host not specified")
            wrapper.connect(
                host = host,
                port = source.port ?: 445,
                username = source.username ?: "",
                password = password,
                domain = source.domain,
                shareName = shareName
            )
        }
    }

    /**
     * 构建完整路径。
     * @param relativePath 相对路径
     * @return 完整的 SMB 路径
     */
    private fun buildFullPath(relativePath: String): String {
        val relative = relativePath.trim('/')
        return listOf(basePath, relative)
            .filter { it.isNotEmpty() }
            .joinToString("/")
    }

    override suspend fun listDirectory(relativePath: String): List<FileEntry> = withContext(Dispatchers.IO) {
        try {
            ensureConnected()
            wrapper.listDirectory(buildFullPath(relativePath))
        } catch (e: SmbConnectionException) {
            throw e
        } catch (e: SmbAuthException) {
            throw e
        }
    }

    override suspend fun stat(relativePath: String): FileEntry? = withContext(Dispatchers.IO) {
        try {
            ensureConnected()
            wrapper.stat(buildFullPath(relativePath))
        } catch (e: SmbConnectionException) {
            throw e
        } catch (e: SmbAuthException) {
            throw e
        }
    }

    override suspend fun readFile(relativePath: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            ensureConnected()
            wrapper.readFile(buildFullPath(relativePath))
        } catch (e: SmbConnectionException) {
            throw e
        } catch (e: SmbAuthException) {
            throw e
        }
    }

    override suspend fun readRange(relativePath: String, offset: Long, length: Long): ByteArray = withContext(Dispatchers.IO) {
        try {
            ensureConnected()
            wrapper.readRange(buildFullPath(relativePath), offset, length)
        } catch (e: SmbConnectionException) {
            throw e
        } catch (e: SmbAuthException) {
            throw e
        }
    }

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            wrapper.testConnection(
                host = source.host ?: return@withContext false,
                port = source.port ?: 445,
                username = source.username ?: "",
                password = password,
                domain = source.domain,
                shareName = shareName
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun openInputStream(relativePath: String): InputStream {
        return try {
            ensureConnected()
            wrapper.openInputStream(buildFullPath(relativePath))
        } catch (e: SmbConnectionException) {
            throw e
        } catch (e: SmbAuthException) {
            throw e
        }
    }

    override fun disconnect() {
        wrapper.disconnect()
    }
}
