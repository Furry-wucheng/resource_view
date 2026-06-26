package dev.wucheng.resource_viewer

import android.app.Application
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.di.coilModule
import dev.wucheng.resource_viewer.di.databaseModule
import dev.wucheng.resource_viewer.di.repositoryModule
import dev.wucheng.resource_viewer.di.securePrefsModule
import dev.wucheng.resource_viewer.di.smbModule
import dev.wucheng.resource_viewer.di.viewModelModule
import dev.wucheng.resource_viewer.ui.base.FatalErrorHolder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin

class ResourceViewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ResourceViewerApp)
            modules(databaseModule, securePrefsModule, repositoryModule, smbModule, viewModelModule, coilModule)
        }
        warmUpDatabase()
    }

    /**
     * 预热数据库，捕获致命异常（如数据库损坏）。
     */
    private fun warmUpDatabase() {
        try {
            val database = getKoin().get<AppDatabase>()
            database.openHelper.writableDatabase
        } catch (e: Exception) {
            FatalErrorHolder.setFatalError("数据库初始化失败：${e.message}")
        }
    }
}
