package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.data.repository.ThumbnailRepository
import dev.wucheng.resource_viewer.domain.usecase.BatchAddResourcesUseCase
import dev.wucheng.resource_viewer.domain.usecase.DetectOrganizationModeUseCase
import dev.wucheng.resource_viewer.domain.usecase.ScanResourcesUseCase
import dev.wucheng.resource_viewer.domain.usecase.SplitResourceUseCase
import dev.wucheng.resource_viewer.shared.thumbnail.FileBrowserThumbnailDiskCache
import dev.wucheng.resource_viewer.shared.thumbnail.ImageThumbnailGenerator
import dev.wucheng.resource_viewer.shared.thumbnail.PdfThumbnailGenerator
import dev.wucheng.resource_viewer.shared.thumbnail.VideoThumbnailGenerator
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
    single { FilesystemRepository(get(), get(), get()) }
    single { DetectOrganizationModeUseCase() }
    single { BatchAddResourcesUseCase(get(), get(), get(), get(), get()) }
    single { ScanResourcesUseCase(get(), get()) }
    single { SplitResourceUseCase(get()) }
    single {
        val diskCache = get<FileBrowserThumbnailDiskCache>()
        ThumbnailRepository(
            setOf(
                VideoThumbnailGenerator(diskCache),
                PdfThumbnailGenerator(get(), diskCache),
                ImageThumbnailGenerator(get(), diskCache),
                // ArchiveThumbnailGenerator (P2)
            )
        )
    }
}
