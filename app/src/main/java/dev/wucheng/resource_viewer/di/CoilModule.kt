@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import dev.wucheng.resource_viewer.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.koin.dsl.module

/**
 * Coil Koin Module。
 * 提供全局 ImageLoader 单例，配置内存缓存和磁盘缓存。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 CoilModule 契约。
 */
val coilModule = module {
    single {
        val context = get<Context>()
        val database = get<AppDatabase>()

        // 从数据库读取缓存配置
        val cacheLimitMB = runBlocking {
            val config = database.appConfigDao().getConfig().first()
            config?.cacheLimitMB ?: 500
        }

        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25) // 内存缓存 25%
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(cacheLimitMB.toLong() * 1024 * 1024) // 根据配置设置磁盘缓存大小
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
