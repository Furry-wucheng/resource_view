package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY createdAt DESC")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: SourceEntity)

    @Update
    suspend fun update(source: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE sources SET isAvailable = :available, lastCheckAt = :checkTime WHERE id = :id")
    suspend fun updateAvailability(id: String, available: Boolean, checkTime: Long)

    @Query("SELECT COUNT(*) FROM resources WHERE sourceId = :sourceId")
    suspend fun getResourceCount(sourceId: String): Int
}
