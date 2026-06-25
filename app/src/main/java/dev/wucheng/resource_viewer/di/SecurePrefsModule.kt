package dev.wucheng.resource_viewer.di

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * SecurePrefs Koin Module。
 * 提供 EncryptedSharedPreferences 和 SecurePrefs 单例。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 SecurePrefsModule 契约。
 */
val securePrefsModule = module {
    single<SharedPreferences> {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    single { SecurePrefs(get()) }
}
