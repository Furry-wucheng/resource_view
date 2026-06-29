@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailLoadManager
import dev.wucheng.resource_viewer.ui.screens.home.HomeViewModel
import dev.wucheng.resource_viewer.ui.screens.settings.SettingsViewModel
import dev.wucheng.resource_viewer.ui.screens.sources.FileBrowserViewModel
import dev.wucheng.resource_viewer.ui.screens.sources.SourceListViewModel
import dev.wucheng.resource_viewer.ui.screens.tags.TagViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ChapterListViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ContentGridMode
import dev.wucheng.resource_viewer.ui.screens.viewer.ContentGridViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { TagViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SourceListViewModel(get(), get(), get(), get()) }
    viewModel { (sourceId: String) ->
        FileBrowserViewModel(sourceId, get(), get(), get(), get(), get(), get())
    }
    viewModel { (resourceId: String, contentPath: String, initialPage: Int) ->
        ViewerViewModel(
            resourceId = resourceId,
            contentPath = contentPath,
            initialPage = initialPage,
            resourceRepository = get(),
            filesystemRepository = get(),
            context = get(),
            appConfigDao = get(),
        )
    }
    viewModel { (resourceId: String) ->
        ChapterListViewModel(resourceId, get(), get(), get(), get())
    }
    viewModel { (resourceId: String, mode: ContentGridMode) ->
        ContentGridViewModel(resourceId, mode, get(), get(), get(), get())
    }

    // VideoPlayerController 不再通过 Koin viewModel 管理，
    // 其生命周期由 Composable (remember + DisposableEffect) 直接控制，
    // 确保视频页离开底层 MediaCodec 立即释放，防止 DecoderInitializationException: NO_MEMORY。
}
