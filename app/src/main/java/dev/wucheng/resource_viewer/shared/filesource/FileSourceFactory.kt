package dev.wucheng.resource_viewer.shared.filesource

import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.domain.model.Source

/**
 * FileSource 工厂对象。
 * 根据 SourceType 返回对应的 FileSource 实现。
 *
 * 注意：此定义来自 doc/share/02-interfaces.md 共享契约。
 * LocalFileSource 将在 M12 实现，SmbFileSource 将在 M17 实现。
 */
object FileSourceFactory {
    fun create(source: Source, password: String? = null): FileSource {
        return when (source.type) {
            SourceType.LOCAL -> LocalFileSource(source.id, source.rootPath)
            SourceType.SMB -> SmbFileSource(source, password ?: throw IllegalArgumentException("SMB source requires password"))
            SourceType.FTP -> throw UnsupportedOperationException("FTP not yet supported")
            SourceType.WEBDAV -> throw UnsupportedOperationException("WebDAV not yet supported")
        }
    }
}

/**
 * 本地文件源占位实现。
 * 完整实现将在 M12 中添加。
 */
private class LocalFileSource(
    override val sourceId: String,
    private val rootPath: String,
) : FileSource {
    override suspend fun listDirectory(relativePath: String): List<dev.wucheng.resource_viewer.domain.model.FileEntry> {
        throw NotImplementedError("LocalFileSource will be implemented in M12")
    }

    override suspend fun stat(relativePath: String): dev.wucheng.resource_viewer.domain.model.FileEntry? {
        throw NotImplementedError("LocalFileSource will be implemented in M12")
    }

    override suspend fun readFile(relativePath: String): ByteArray {
        throw NotImplementedError("LocalFileSource will be implemented in M12")
    }

    override suspend fun readRange(relativePath: String, offset: Long, length: Long): ByteArray {
        throw NotImplementedError("LocalFileSource will be implemented in M12")
    }

    override fun openInputStream(relativePath: String): java.io.InputStream {
        throw NotImplementedError("LocalFileSource will be implemented in M12")
    }

    override suspend fun testConnection(): Boolean {
        throw NotImplementedError("LocalFileSource will be implemented in M12")
    }

    override fun disconnect() {
        // No-op for placeholder
    }
}

/**
 * SMB 文件源占位实现。
 * 完整实现将在 M17 中添加。
 */
private class SmbFileSource(
    private val source: Source,
    private val password: String,
) : FileSource {
    override val sourceId: String = source.id

    override suspend fun listDirectory(relativePath: String): List<dev.wucheng.resource_viewer.domain.model.FileEntry> {
        throw NotImplementedError("SmbFileSource will be implemented in M17")
    }

    override suspend fun stat(relativePath: String): dev.wucheng.resource_viewer.domain.model.FileEntry? {
        throw NotImplementedError("SmbFileSource will be implemented in M17")
    }

    override suspend fun readFile(relativePath: String): ByteArray {
        throw NotImplementedError("SmbFileSource will be implemented in M17")
    }

    override suspend fun readRange(relativePath: String, offset: Long, length: Long): ByteArray {
        throw NotImplementedError("SmbFileSource will be implemented in M17")
    }

    override fun openInputStream(relativePath: String): java.io.InputStream {
        throw NotImplementedError("SmbFileSource will be implemented in M17")
    }

    override suspend fun testConnection(): Boolean {
        throw NotImplementedError("SmbFileSource will be implemented in M17")
    }

    override fun disconnect() {
        // No-op for placeholder
    }
}
