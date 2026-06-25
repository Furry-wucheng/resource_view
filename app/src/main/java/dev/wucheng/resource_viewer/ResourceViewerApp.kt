package dev.wucheng.resource_viewer

import android.app.Application
import dev.wucheng.resource_viewer.di.databaseModule
import dev.wucheng.resource_viewer.di.repositoryModule
import dev.wucheng.resource_viewer.di.securePrefsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ResourceViewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ResourceViewerApp)
            modules(databaseModule, securePrefsModule, repositoryModule)
        }
    }
}
