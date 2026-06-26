package dev.wucheng.resource_viewer.data.repository

import dev.wucheng.resource_viewer.data.local.dao.SourceDao
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.asErr
import dev.wucheng.resource_viewer.domain.error.asOk
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.data.local.entity.toDomain
import dev.wucheng.resource_viewer.shared.filesource.FileSourceFactory

/**
 * 文件系统仓库。
 * 基于数据源配置提供文件浏览功能。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 RepositoryModule 契约。
 */
class FilesystemRepository(
    private val sourceDao: SourceDao,
    private val securePrefs: SecurePrefs,
) {
    /**
     * 列出指定数据源的目录内容。
     * @param source 数据源
     * @param relativePath 相对路径
     */
    suspend fun listDirectory(source: Source, relativePath: String): Result<List<FileEntry>> {
        return try {
            val password = securePrefs.getPassword(source.id)
            val fileSource = FileSourceFactory.create(source, password)
            val entries = fileSource.listDirectory(relativePath)
            entries.asOk()
        } catch (e: Exception) {
            DomainError.FileNotFoundError("Failed to list directory", e).asErr()
        }
    }

    /**
     * 获取文件/目录元数据。
     * @param source 数据源
     * @param relativePath 相对路径
     */
    suspend fun stat(source: Source, relativePath: String): Result<FileEntry?> {
        return try {
            val password = securePrefs.getPassword(source.id)
            val fileSource = FileSourceFactory.create(source, password)
            val entry = fileSource.stat(relativePath)
            entry.asOk()
        } catch (e: Exception) {
            DomainError.FileNotFoundError("Failed to stat file", e).asErr()
        }
    }

    /**
     * 获取数据源密码。
     * @param sourceId 数据源 ID
     * @return 密码字符串，如果未存储则返回 null
     */
    fun getPassword(sourceId: String): String? {
        return securePrefs.getPassword(sourceId)
    }

    /**
     * 测试数据源连接。
     * @param source 数据源
     */
    suspend fun testConnection(source: Source): Result<Boolean> {
        return try {
            val password = securePrefs.getPassword(source.id)
            val fileSource = FileSourceFactory.create(source, password)
            fileSource.testConnection().asOk()
        } catch (e: Exception) {
            DomainError.SourceUnreachableError("Connection test failed", e).asErr()
        }
    }

    /**
     * 根据 ID 获取数据源。
     * @param sourceId 数据源 ID
     */
    suspend fun getSource(sourceId: String): Result<Source?> {
        return try {
            val entity = sourceDao.getSourceById(sourceId)
            entity?.toDomain().asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to get source", e).asErr()
        }
    }

    /**
     * 根据 ID 获取数据源并创建 FileSource。
     * @param sourceId 数据源 ID
     */
    suspend fun getFileSource(sourceId: String): Result<FileSource> {
        return try {
            val entity = sourceDao.getSourceById(sourceId)
                ?: return DomainError.DatabaseError("Source not found").asErr()
            val source = entity.toDomain()
            val password = securePrefs.getPassword(sourceId)
            FileSourceFactory.create(source, password).asOk()
        } catch (e: Exception) {
            DomainError.SourceUnreachableError("Failed to create file source", e).asErr()
        }
    }
}

