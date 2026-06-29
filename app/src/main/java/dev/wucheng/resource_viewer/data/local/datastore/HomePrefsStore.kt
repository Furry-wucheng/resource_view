package dev.wucheng.resource_viewer.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore(name = "home_prefs")

class HomePrefsStore(private val context: Context) {

    companion object {
        private val KEY_RESOURCE_SORT = stringPreferencesKey("resource_sort")
    }

    suspend fun loadResourceSort(): String? {
        return context.homeDataStore.data.first()[KEY_RESOURCE_SORT]
    }

    suspend fun saveResourceSort(sort: String) {
        context.homeDataStore.edit { it[KEY_RESOURCE_SORT] = sort }
    }
}
