package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.ui.screens.tags.TagViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel Koin Module。
 * 提供所有 ViewModel。
 */
val viewModelModule = module {
    viewModel { TagViewModel(get()) }
}
