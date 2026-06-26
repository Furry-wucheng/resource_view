package dev.wucheng.resource_viewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getConfig(): Flow<AppConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: AppConfigEntity)

    // M12: 检查隐私政策是否已同意
    @Query("SELECT hasAcceptedPrivacy FROM app_config WHERE id = 1")
    suspend fun hasAcceptedPrivacy(): Boolean?

    // M12: 更新隐私政策同意状态
    @Query("UPDATE app_config SET hasAcceptedPrivacy = :accepted, updatedAt = :timestamp WHERE id = 1")
    suspend fun updatePrivacyAccepted(accepted: Boolean, timestamp: Long = System.currentTimeMillis())
}
