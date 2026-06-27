package dev.wucheng.resource_viewer.data.repository

import dev.wucheng.resource_viewer.data.local.dao.SourceDao
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.entity.toDomain
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.asErr
import dev.wucheng.resource_viewer.domain.error.asOk
import dev.wucheng.resource_viewer.domain.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 数据源仓库。
 * 提供数据源的 CRUD 操作和密码管理。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 RepositoryModule 契约。
 */
class SourceRepository(
    private val sourceDao: SourceDao,
    private val securePrefs: SecurePrefs,
) {
    /**
     * 获取所有数据源（Flow）。
     */
    fun getAllSources(): Flow<List<Source>> {
        return sourceDao.getAllSources().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 根据 ID 获取数据源。
     */
    suspend fun getSourceById(id: String): Result<Source?> {
        return try {
            val entity = sourceDao.getSourceById(id)
            entity?.toDomain().asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to get source by id", e).asErr()
        }
    }

    /**
     * 插入数据源。
     */
    suspend fun insert(source: SourceEntity): Result<Unit> {
        return try {
            sourceDao.insert(source).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to insert source", e).asErr()
        }
    }

    /**
     * 更新数据源。
     */
    suspend fun update(source: SourceEntity): Result<Unit> {
        return try {
            sourceDao.update(source).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to update source", e).asErr()
        }
    }

    /**
     * 删除数据源。
     */
    suspend fun deleteById(id: String): Result<Unit> {
        return try {
            sourceDao.deleteById(id).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to delete source", e).asErr()
        }
    }

    /**
     * 更新数据源可用性状态。
     */
    suspend fun updateAvailability(id: String, available: Boolean, checkTime: Long): Result<Unit> {
        return try {
            sourceDao.updateAvailability(id, available, checkTime).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to update source availability", e).asErr()
        }
    }

    /**
     * 存储数据源密码。
     */
    fun putPassword(sourceId: String, password: String) {
        securePrefs.putPassword(sourceId, password)
    }

    /**
     * 获取数据源密码。
     */
    fun getPassword(sourceId: String): String? {
        return securePrefs.getPassword(sourceId)
    }

    /**
     * 移除数据源密码。
     */
    fun removePassword(sourceId: String) {
        securePrefs.removePassword(sourceId)
    }

    /**
     * 获取数据源下的资源数量。
     */
    suspend fun getResourceCount(sourceId: String): Int {
        return try {
            sourceDao.getResourceCount(sourceId)
        } catch (e: Exception) {
            0
        }
    }
}
