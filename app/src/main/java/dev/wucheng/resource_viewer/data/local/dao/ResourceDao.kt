package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun getVisibleResources(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE isAvailable = 1 ORDER BY createdAt DESC")
    fun getAvailableResources(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getById(id: String): ResourceEntity?

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

    @Query("SELECT * FROM resources WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ResourceEntity>>
}
