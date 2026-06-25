package dev.wucheng.resource_viewer.data.repository

import dev.wucheng.resource_viewer.data.local.dao.ResourceTagDao
import dev.wucheng.resource_viewer.data.local.dao.TagDao
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.data.local.entity.toDomain
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.asErr
import dev.wucheng.resource_viewer.domain.error.asOk
import dev.wucheng.resource_viewer.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 标签仓库。
 * 提供标签的 CRUD 操作和资源统计。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 RepositoryModule 契约。
 */
class TagRepository(
    private val tagDao: TagDao,
    private val resourceTagDao: ResourceTagDao,
) {
    /**
     * 获取所有标签（Flow）。
     */
    fun getAllTags(): Flow<List<Tag>> {
        return combine(
            tagDao.getAllTags(),
            tagDao.getTagResourceCounts(),
        ) { tags, counts ->
            tags.map { entity ->
                val count = counts.find { it.id == entity.id }?.count ?: 0
                entity.toDomain(count)
            }
        }
    }

    /**
     * 根据 ID 获取标签。
     */
    suspend fun getById(id: String): Result<Tag?> {
        return try {
            val entity = tagDao.getById(id)
            if (entity != null) {
                val count = resourceTagDao.countByTagId(id)
                entity.toDomain(count).asOk()
            } else {
                null.asOk()
            }
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to get tag by id", e).asErr()
        }
    }

    /**
     * 插入标签。
     */
    suspend fun insert(tag: TagEntity): Result<Unit> {
        return try {
            tagDao.insert(tag).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to insert tag", e).asErr()
        }
    }

    /**
     * 更新标签。
     */
    suspend fun update(tag: TagEntity): Result<Unit> {
        return try {
            if (tag.isBuiltIn) {
                DomainError.ValidationError("Cannot update built-in tag").asErr()
            } else {
                tagDao.update(tag).asOk()
            }
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to update tag", e).asErr()
        }
    }

    /**
     * 删除标签。
     */
    suspend fun deleteById(id: String): Result<Unit> {
        return try {
            val tag = tagDao.getById(id)
            if (tag == null) {
                DomainError.DatabaseError("Tag not found").asErr()
            } else if (tag.isBuiltIn) {
                DomainError.ValidationError("Cannot delete built-in tag").asErr()
            } else {
                tagDao.deleteById(id).asOk()
            }
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to delete tag", e).asErr()
        }
    }

    /**
     * 获取标签资源统计（Flow）。
     */
    fun getTagResourceCounts(): Flow<List<Pair<String, Int>>> {
        return tagDao.getTagResourceCounts().let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { counts ->
                    emit(counts.map { it.id to it.count })
                }
            }
        }
    }
}
