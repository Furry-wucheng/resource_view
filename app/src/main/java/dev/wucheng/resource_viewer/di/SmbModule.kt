package dev.wucheng.resource_viewer.di

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import dev.wucheng.resource_viewer.data.remote.smb.SmbClientWrapper
import org.koin.dsl.module

/**
 * SMB Koin Module。
 * 提供 SmbClientWrapper 单例。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 SmbModule 契约。
 */
val smbModule = module {
    single<SMBClient> {
        val config = SmbConfig.builder().build()
        SMBClient(config)
    }

    single { SmbClientWrapper(get()) }
}
