@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.di

import androidx.media3.exoplayer.ExoPlayer
import dev.wucheng.resource_viewer.ui.screens.settings.SettingsViewModel
import dev.wucheng.resource_viewer.ui.screens.sources.SourceListViewModel
import dev.wucheng.resource_viewer.ui.screens.tags.TagViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.VideoPlayerViewModel
import dev.wucheng.resource_viewer.ui.screens.viewer.ViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel Koin Module。
 * 提供所有 ViewModel 实例。
 */
val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { TagViewModel(get()) }
    viewModel { SourceListViewModel(get(), get(), get()) }
    viewModel { (resourceId: String) -> ViewerViewModel(resourceId, get(), get()) }

    // M19: VideoPlayerViewModel — 每次创建新实例（含独立 ExoPlayer）
    viewModel<VideoPlayerViewModel> {
        val context = get<android.content.Context>()
        val player = ExoPlayer.Builder(context).build()
        VideoPlayerViewModel(player)
    }
}
