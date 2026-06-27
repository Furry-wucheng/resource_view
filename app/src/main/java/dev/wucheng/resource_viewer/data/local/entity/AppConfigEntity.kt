package dev.wucheng.resource_viewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import dev.wucheng.resource_viewer.domain.model.AppConfig

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pageDirection: PageDirection = PageDirection.RIGHT_TO_LEFT,
    val doublePageMode: DoublePageMode = DoublePageMode.AUTO,
    val crossChapter: Boolean = true,
    val cacheLimitMB: Int = 500,
    val thumbnailConcurrency: Int = 4,
    val autoSyncInterval: AutoSyncInterval? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    // M12: 隐私政策同意状态
    val hasAcceptedPrivacy: Boolean = false,
    // 缓存容量设置（0 表示无限制）
    val coverCacheLimitMB: Int = 0,      // 封面缓存容量，默认无限制（永久）
    val pageCacheLimitMB: Int = 500,     // 页面缓存容量，默认 500MB
    val thumbnailCacheLimitMB: Int = 500, // 缩略图缓存容量，默认 500MB
)

fun AppConfigEntity.toDomain(): AppConfig = AppConfig(
    themeMode = themeMode,
    pageDirection = pageDirection,
    doublePageMode = doublePageMode,
    crossChapter = crossChapter,
    cacheLimitMB = cacheLimitMB,
    thumbnailConcurrency = thumbnailConcurrency,
    autoSyncInterval = autoSyncInterval,
    coverCacheLimitMB = coverCacheLimitMB,
    pageCacheLimitMB = pageCacheLimitMB,
    thumbnailCacheLimitMB = thumbnailCacheLimitMB,
)
