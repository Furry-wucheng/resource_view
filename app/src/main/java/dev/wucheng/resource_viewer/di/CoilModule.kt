@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import dev.wucheng.resource_viewer.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.koin.dsl.module
import dev.wucheng.resource_viewer.shared.thumbnail.FileBrowserThumbnailDiskCache
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailLoadManager

val coilModule = module {
    single { FileBrowserThumbnailDiskCache(get()) }
    single {
        val database = get<AppDatabase>()
        val config = runBlocking { database.appConfigDao().getConfig().first() }
        val loadManager = ThumbnailLoadManager(
            context = get(),
            diskCache = get(),
            maxConcurrency = config?.thumbnailConcurrency ?: 4,
            maxCacheSize = 64,
        )
        loadManager.configureCapacity(config?.thumbnailCacheLimitMB ?: 500)
        loadManager
    }
    single {
        val context = get<Context>()
        val database = get<AppDatabase>()

        val cacheLimitMB = runBlocking {
            val config = database.appConfigDao().getConfig().first()
            config?.thumbnailCacheLimitMB ?: 500
        }

        ImageLoader.Builder(context)
            .components {
                add(AnimatedImageDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache/coil").toOkioPath())
                    .maxSizeBytes(cacheLimitMB.toLong() * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
