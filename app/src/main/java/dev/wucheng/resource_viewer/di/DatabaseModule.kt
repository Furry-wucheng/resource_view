package dev.wucheng.resource_viewer.di

import androidx.room.Room
import dev.wucheng.resource_viewer.data.local.AppDatabase
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "resource_viewer.db"
        ).build()
    }
}
