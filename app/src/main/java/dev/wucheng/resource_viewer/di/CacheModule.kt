package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.data.cache.CacheManager
import org.koin.dsl.module

/**
 * Cache Koin Module。
 * 提供 CacheManager 单例，统一管理各类缓存。
 */
val cacheModule = module {
    single { CacheManager(get(), get(), get()) }
}
