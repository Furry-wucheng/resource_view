package dev.wucheng.resource_viewer.di

import dev.wucheng.resource_viewer.data.local.datastore.FileBrowserPrefsStore
import dev.wucheng.resource_viewer.data.local.datastore.HomePrefsStore
import org.koin.dsl.module

val dataStoreModule = module {
    single { FileBrowserPrefsStore(get()) }
    single { HomePrefsStore(get()) }
}
