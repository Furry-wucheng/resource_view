package dev.wucheng.resource_viewer.di

import androidx.room.Room
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.backup.DatabaseBackupManager
import dev.wucheng.resource_viewer.data.local.migration.DatabaseMigrator
import org.koin.dsl.module

val databaseModule = module {
    single {
        val migrator = DatabaseMigrator(get())
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*migrator.getMigrations())
            .build()
    }

    single { get<AppDatabase>().sourceDao() }
    single { get<AppDatabase>().resourceDao() }
    single { get<AppDatabase>().tagDao() }
    single { get<AppDatabase>().resourceTagDao() }
    single { get<AppDatabase>().appConfigDao() }

    single { DatabaseMigrator(get()) }
    single { DatabaseBackupManager(get(), get()) }
}
