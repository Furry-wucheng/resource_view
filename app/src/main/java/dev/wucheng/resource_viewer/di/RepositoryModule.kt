package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.data.repository.ThumbnailRepository
import org.koin.dsl.module

/**
 * Repository Koin Module。
 * 提供所有 Repository 单例。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 RepositoryModule 契约。
 */
val repositoryModule = module {
    single { SourceRepository(get(), get()) }
    single { ResourceRepository(get(), get(), get()) }
    single { TagRepository(get(), get()) }
    single { FilesystemRepository(get(), get()) }
    single { ThumbnailRepository(emptySet()) } // 实际生成器将在后续 Stage 添加
}

