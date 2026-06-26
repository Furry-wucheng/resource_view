package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.ui.screens.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel Koin Module。
 * 提供所有 ViewModel 实例。
 */
val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get()) }
}
