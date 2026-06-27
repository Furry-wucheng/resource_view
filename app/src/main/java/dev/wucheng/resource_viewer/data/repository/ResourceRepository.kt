package dev.wucheng.resource_viewer.data.repository

import dev.wucheng.resource_viewer.data.local.dao.ResourceDao
import dev.wucheng.resource_viewer.data.local.dao.ResourceTagDao
import dev.wucheng.resource_viewer.data.local.dao.TagDao
import dev.wucheng.resource_viewer.data.local.dao.ResourceTagWithTag
import dev.wucheng.resource_viewer.data.local.dao.ResourceWithSourceName
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.local.entity.toDomain
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.asErr
import dev.wucheng.resource_viewer.domain.error.asOk
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 资源仓库。
 * 提供资源的 CRUD 操作、标签筛选和搜索功能。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 RepositoryModule 契约。
 */
class ResourceRepository(
    private val resourceDao: ResourceDao,
    private val tagDao: TagDao,
    private val resourceTagDao: ResourceTagDao,
) {
    /**
     * 获取所有可见资源（Flow）。
     */
    fun getVisibleResources(): Flow<List<Resource>> {
        return combine(
            resourceDao.getVisibleResourceItems(),
            tagDao.getAllResourceTagsWithTags(),
        ) { resources, tagRows ->
            resources.toDomainList(tagRows)
        }
    }

    /**
     * 获取可用资源（Flow）。
     */
    fun getAvailableResources(): Flow<List<Resource>> {
        return combine(
            resourceDao.getAvailableResourceItems(),
            tagDao.getAllResourceTagsWithTags(),
        ) { resources, tagRows ->
            resources.toDomainList(tagRows)
        }
    }

    /**
     * 根据 ID 获取资源。
     */
    suspend fun getById(id: String): Result<Resource?> {
        return try {
            val item = resourceDao.getByIdWithSource(id)
            if (item != null) {
                val tagCounts = tagDao.getTagResourceCountsSnapshot()
                item.resource.toDomain(
                    sourceName = item.sourceName,
                    tags = getTagsForResource(item.resource.id, tagCounts),
                ).asOk()
            } else {
                null.asOk()
            }
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to get resource by id", e).asErr()
        }
    }

    /**
     * 插入资源。
     */
    suspend fun insert(resource: ResourceEntity): Result<Unit> {
        return try {
            resourceDao.insert(resource).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to insert resource", e).asErr()
        }
    }

    /**
     * 批量插入资源。
     */
    suspend fun insertAll(resources: List<ResourceEntity>): Result<Unit> {
        return try {
            resourceDao.insertAll(resources).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to insert resources", e).asErr()
        }
    }

    /**
     * 更新资源。
     */
    suspend fun update(resource: ResourceEntity): Result<Unit> {
        return try {
            resourceDao.update(resource).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to update resource", e).asErr()
        }
    }

    /**
     * 删除资源。
     */
    suspend fun deleteById(id: String): Result<Unit> {
        return try {
            resourceDao.deleteById(id).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to delete resource", e).asErr()
        }
    }

    /**
     * 按来源删除资源。
     */
    suspend fun deleteBySourceId(sourceId: String): Result<Unit> {
        return try {
            resourceDao.deleteBySourceId(sourceId).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to delete resources by source", e).asErr()
        }
    }

    /**
     * 更新资源组织模式。
     */
    suspend fun updateOrganizationMode(id: String, organizationMode: OrganizationMode?): Result<Unit> {
        return try {
            resourceDao.updateOrganizationMode(id, organizationMode, System.currentTimeMillis()).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to update resource organization mode", e).asErr()
        }
    }

    /**
     * 切换资源收藏状态。
     */
    suspend fun toggleFavorite(id: String, favorited: Boolean): Result<Unit> {
        return try {
            resourceDao.updateFavorite(id, favorited, System.currentTimeMillis()).asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to toggle favorite", e).asErr()
        }
    }

    /**
     * 键集分页获取资源。
     */
    suspend fun pageAfter(beforeCreatedAt: Long, limit: Int): Result<List<Resource>> {
        return try {
            val items = resourceDao.pageAfterWithSource(beforeCreatedAt, limit)
            val tagCounts = tagDao.getTagResourceCountsSnapshot()
            items.map { item ->
                item.resource.toDomain(
                    sourceName = item.sourceName,
                    tags = getTagsForResource(item.resource.id, tagCounts),
                )
            }.asOk()
        } catch (e: Exception) {
            DomainError.DatabaseError("Failed to page resources", e).asErr()
        }
    }

    /**
     * 按标签筛选资源（交集查询）。
     */
    fun filterByTags(tagIds: List<String>): Flow<List<Resource>> {
        return combine(
            resourceDao.filterResourceItemsByTags(tagIds, tagIds.size),
            tagDao.getAllResourceTagsWithTags(),
        ) { resources, tagRows ->
            resources.toDomainList(tagRows)
        }
    }

    /**
     * 按名称搜索资源。
     */
    fun searchByName(query: String): Flow<List<Resource>> {
        return combine(
            resourceDao.searchResourceItemsByName(query),
            tagDao.getAllResourceTagsWithTags(),
        ) { resources, tagRows ->
            resources.toDomainList(tagRows)
        }
    }

    /**
     * 获取资源关联的标签。
     */
    private suspend fun getTagsForResource(resourceId: String, tagCounts: List<dev.wucheng.resource_viewer.data.local.dao.TagCount>): List<Tag> {
        val resourceTags = resourceTagDao.getByResourceId(resourceId)
        return resourceTags.map { rt ->
            val tagEntity = tagDao.getById(rt.tagId)
            val count = tagCounts.find { it.id == rt.tagId }?.count ?: 0
            tagEntity?.toDomain(count) ?: Tag(
                id = rt.tagId,
                name = "Unknown",
                color = "#000000",
                createdAt = 0,
                updatedAt = 0,
            )
        }
    }

    private fun List<ResourceWithSourceName>.toDomainList(tagRows: List<ResourceTagWithTag>): List<Resource> {
        val countByTagId = tagRows.groupingBy { it.id }.eachCount()
        val tagsByResourceId = tagRows
            .groupBy { it.resourceId }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    Tag(
                        id = row.id,
                        name = row.name,
                        color = row.color,
                        isBuiltIn = row.isBuiltIn,
                        resourceCount = countByTagId[row.id] ?: 0,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt,
                    )
                }
            }

        return map { item ->
            item.resource.toDomain(
                sourceName = item.sourceName,
                tags = tagsByResourceId[item.resource.id].orEmpty(),
            )
        }
    }
}
