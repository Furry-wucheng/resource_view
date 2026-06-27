@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.di

import androidx.media3.exoplayer.ExoPlayer
import dev.wucheng.resource_viewer.ui.screens.home.HomeViewModel
import dev.wucheng.resource_viewer.ui.screens.settings.SettingsViewModel
import dev.wucheng.resource_viewer.ui.screens.sources.FileBrowserViewModel
import dev.wucheng.resource_viewer.ui.screens.sources.SourceListViewModel
import dev.wucheng.resource_viewer.ui.screens.tags.TagViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ChapterListViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ContentGridMode
import dev.wucheng.resource_viewer.ui.screens.viewer.ContentGridViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.VideoPlayerViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel Koin Module。
 * 提供所有 ViewModel 实例。
 */
val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { TagViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SourceListViewModel(get(), get(), get(), get()) }
    viewModel { (sourceId: String) -> FileBrowserViewModel(sourceId, get(), get(), get()) }
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
    viewModel { (resourceId: String) -> ChapterListViewModel(resourceId, get(), get()) }
    viewModel { (resourceId: String, mode: ContentGridMode) ->
        ContentGridViewModel(resourceId, mode, get(), get())
    }

    // M19: VideoPlayerViewModel — 每次创建新实例（含独立 ExoPlayer）
    viewModel<VideoPlayerViewModel> {
        val context = get<android.content.Context>()
        val player = ExoPlayer.Builder(context).build()
        VideoPlayerViewModel(player)
    }
}
