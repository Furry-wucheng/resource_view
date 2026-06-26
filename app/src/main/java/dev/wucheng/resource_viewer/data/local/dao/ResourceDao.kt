package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

data class ResourceWithSourceName(
    @Embedded val resource: ResourceEntity,
    val sourceName: String,
)

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun getVisibleResources(): Flow<List<ResourceEntity>>

    @Query("""
        SELECT r.*, s.name AS sourceName
        FROM resources r
        INNER JOIN sources s ON s.id = r.sourceId
        ORDER BY r.createdAt DESC
    """)
    fun getVisibleResourceItems(): Flow<List<ResourceWithSourceName>>

    @Query("SELECT * FROM resources WHERE isAvailable = 1 ORDER BY createdAt DESC")
    fun getAvailableResources(): Flow<List<ResourceEntity>>

    @Query("""
        SELECT r.*, s.name AS sourceName
        FROM resources r
        INNER JOIN sources s ON s.id = r.sourceId
        WHERE r.isAvailable = 1
        ORDER BY r.createdAt DESC
    """)
    fun getAvailableResourceItems(): Flow<List<ResourceWithSourceName>>

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getById(id: String): ResourceEntity?

    @Query("""
        SELECT r.*, s.name AS sourceName
        FROM resources r
        INNER JOIN sources s ON s.id = r.sourceId
        WHERE r.id = :id
    """)
    suspend fun getByIdWithSource(id: String): ResourceWithSourceName?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(resource: ResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<ResourceEntity>)

    @Update
    suspend fun update(resource: ResourceEntity)

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM resources WHERE sourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: String)

    @Query("UPDATE resources SET organizationMode = :organizationMode, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrganizationMode(id: String, organizationMode: OrganizationMode?, updatedAt: Long)

    /**
     * 键集分页：获取 createdAt < [beforeCreatedAt] 的资源，
     * 按 createdAt DESC, id ASC 排序，返回 [limit] 条。
     */
    @Query("""
        SELECT * FROM resources
        WHERE createdAt < :beforeCreatedAt
        ORDER BY createdAt DESC, id ASC
        LIMIT :limit
    """)
    suspend fun pageAfter(beforeCreatedAt: Long, limit: Int): List<ResourceEntity>

    @Query("""
        SELECT r.*, s.name AS sourceName
        FROM resources r
        INNER JOIN sources s ON s.id = r.sourceId
        WHERE r.createdAt < :beforeCreatedAt
        ORDER BY r.createdAt DESC, r.id ASC
        LIMIT :limit
    """)
    suspend fun pageAfterWithSource(beforeCreatedAt: Long, limit: Int): List<ResourceWithSourceName>

    /**
     * 标签交集查询：返回同时拥有 [tagIds] 中所有标签的资源。
     * 使用 GROUP BY HAVING COUNT(DISTINCT tagId) = [tagCount] 实现交集。
     */
    @Query("""
        SELECT * FROM resources WHERE id IN (
            SELECT resourceId FROM resource_tags
            WHERE tagId IN (:tagIds)
            GROUP BY resourceId
            HAVING COUNT(DISTINCT tagId) = :tagCount
        )
        ORDER BY createdAt DESC
    """)
    fun filterByTags(tagIds: List<String>, tagCount: Int): Flow<List<ResourceEntity>>

    @Query("""
        SELECT r.*, s.name AS sourceName
        FROM resources r
        INNER JOIN sources s ON s.id = r.sourceId
        WHERE r.id IN (
            SELECT resourceId FROM resource_tags
            WHERE tagId IN (:tagIds)
            GROUP BY resourceId
            HAVING COUNT(DISTINCT tagId) = :tagCount
        )
        ORDER BY r.createdAt DESC
    """)
    fun filterResourceItemsByTags(tagIds: List<String>, tagCount: Int): Flow<List<ResourceWithSourceName>>

    @Query("SELECT * FROM resources WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ResourceEntity>>

    @Query("""
        SELECT r.*, s.name AS sourceName
        FROM resources r
        INNER JOIN sources s ON s.id = r.sourceId
        WHERE r.name LIKE '%' || :query || '%'
        ORDER BY r.name ASC
    """)
    fun searchResourceItemsByName(query: String): Flow<List<ResourceWithSourceName>>
}
