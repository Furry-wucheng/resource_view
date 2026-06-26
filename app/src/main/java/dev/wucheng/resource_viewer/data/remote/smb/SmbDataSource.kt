package dev.wucheng.resource_viewer.data.remote.smb

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import dev.wucheng.resource_viewer.domain.model.Source
import java.io.InputStream

/**
 * SMB DataSource 实现。
 * 将 ExoPlayer 的读取请求桥接到 smbj 文件流。
 *
 * 注意：此实现遵循 doc/mvp/M18-smb-video-datasource.md 中的 M18.1 子任务。
 * read() 是同步方法，直接使用 smbj 的同步阻塞 API，不经过协程。
 */
@androidx.media3.common.util.UnstableApi
class SmbDataSource(
    private val source: Source,
    private val password: String,
    private val wrapper: SmbClientWrapper,
) : DataSource {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0L
    private var opened = false

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
     * @param uri URI
     * @return 完整的 SMB 路径
     */
    private fun buildFullPath(uri: Uri): String {
        val relativePath = uri.path?.trimStart('/') ?: ""
        return listOf(basePath, relativePath.trim('/'))
            .filter { it.isNotEmpty() }
            .joinToString("/")
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // No-op: Transfer listeners not supported for SMB DataSource
    }

    override fun open(dataSpec: DataSpec): Long {
        return try {
            ensureConnected()
            val fullPath = buildFullPath(dataSpec.uri)
            inputStream = wrapper.openInputStream(fullPath)

            // Skip to the requested position
            if (dataSpec.position > 0) {
                val skipped = inputStream?.skip(dataSpec.position) ?: 0L
                if (skipped < dataSpec.position) {
                    throw SmbFileException("Failed to skip to position ${dataSpec.position}")
                }
            }

            // Calculate remaining bytes
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                // Try to get file size from stat
                val entry = wrapper.stat(fullPath)
                entry?.size?.minus(dataSpec.position) ?: C.LENGTH_UNSET.toLong()
            }

            opened = true
            bytesRemaining
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened) return C.RESULT_END_OF_INPUT

        return try {
            val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                minOf(readLength.toLong(), bytesRemaining).toInt()
            } else {
                readLength
            }

            if (bytesToRead <= 0) {
                return C.RESULT_END_OF_INPUT
            }

            val bytesRead = inputStream?.read(buffer, offset, bytesToRead) ?: -1

            if (bytesRead == -1) {
                return C.RESULT_END_OF_INPUT
            }

            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                bytesRemaining -= bytesRead
            }

            bytesRead
        } catch (e: Exception) {
            throw SmbFileException("Failed to read data: ${e.message}", e)
        }
    }

    override fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        inputStream = null
        bytesRemaining = 0L
        opened = false
    }

    override fun getUri(): Uri? {
        return null // SMB 没有标准 URI
    }
}
