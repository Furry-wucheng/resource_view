package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

data class TagCount(val id: String, val count: Int)

data class ResourceTagWithTag(
    val resourceId: String,
    val id: String,
    val name: String,
    val color: String,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY isBuiltIn DESC, createdAt DESC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE isBuiltIn = 1")
    suspend fun getBuiltInTags(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT t.id, COUNT(rt.resourceId) as count
        FROM tags t LEFT JOIN resource_tags rt ON t.id = rt.tagId
        GROUP BY t.id
    """)
    fun getTagResourceCounts(): Flow<List<TagCount>>

    @Query("""
        SELECT t.id, COUNT(rt.resourceId) as count
        FROM tags t LEFT JOIN resource_tags rt ON t.id = rt.tagId
        GROUP BY t.id
    """)
    suspend fun getTagResourceCountsSnapshot(): List<TagCount>

    @Query("""
        SELECT rt.resourceId, t.id, t.name, t.color, t.isBuiltIn, t.createdAt, t.updatedAt
        FROM resource_tags rt
        INNER JOIN tags t ON t.id = rt.tagId
    """)
    fun getAllResourceTagsWithTags(): Flow<List<ResourceTagWithTag>>
}
