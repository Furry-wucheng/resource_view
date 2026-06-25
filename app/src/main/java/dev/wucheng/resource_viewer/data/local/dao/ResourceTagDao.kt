package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity

@Dao
interface ResourceTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(resourceTag: ResourceTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(resourceTags: List<ResourceTagEntity>)

    @Delete
    suspend fun delete(resourceTag: ResourceTagEntity)

    @Query("DELETE FROM resource_tags WHERE resourceId = :resourceId AND tagId = :tagId")
    suspend fun deleteByResourceAndTag(resourceId: String, tagId: String)

    @Query("DELETE FROM resource_tags WHERE resourceId = :resourceId")
    suspend fun deleteByResourceId(resourceId: String)

    @Query("DELETE FROM resource_tags WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: String)

    @Query("SELECT * FROM resource_tags WHERE resourceId = :resourceId")
    suspend fun getByResourceId(resourceId: String): List<ResourceTagEntity>

    @Query("SELECT COUNT(*) FROM resource_tags WHERE tagId = :tagId")
    suspend fun countByTagId(tagId: String): Int
}
