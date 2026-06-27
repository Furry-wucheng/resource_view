package dev.wucheng.resource_viewer.domain.model

import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode

data class AppConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pageDirection: PageDirection = PageDirection.RIGHT_TO_LEFT,
    val doublePageMode: DoublePageMode = DoublePageMode.AUTO,
    val crossChapter: Boolean = true,
    val cacheLimitMB: Int = 500,
    val thumbnailConcurrency: Int = 4,
    val autoSyncInterval: AutoSyncInterval? = null,
    // 缓存容量设置（0 表示无限制）
    val coverCacheLimitMB: Int = 0,      // 封面缓存容量，默认无限制（永久）
    val pageCacheLimitMB: Int = 500,     // 页面缓存容量，默认 500MB
    val thumbnailCacheLimitMB: Int = 500, // 缩略图缓存容量，默认 500MB
    // 文件浏览器设置
    val showDirectoryTree: Boolean = true, // 是否显示目录树导航栏
)
