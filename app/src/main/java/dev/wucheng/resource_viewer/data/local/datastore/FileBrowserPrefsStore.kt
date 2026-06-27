package dev.wucheng.resource_viewer.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "file_browser_prefs")

enum class FileViewMode { LIST, GRID }

enum class FileSortMode {
    NAME_ASC,
    NAME_DESC,
    MODIFIED_ASC,
    MODIFIED_DESC
}

data class FolderPrefs(
    val viewMode: FileViewMode = FileViewMode.GRID,
    val sortMode: FileSortMode = FileSortMode.NAME_ASC,
    val lastAccess: Long = System.currentTimeMillis()
)

class FileBrowserPrefsStore(private val context: Context) {

    companion object {
        private const val MAX_RECORDS = 1000
        private const val KEY_PREFIX = "fb_"
        private const val SEPARATOR = "|"
    }

    private fun makeKey(sourceId: String, path: String): String {
        val pathHash = path.hashCode().toString(16)
        return "$KEY_PREFIX${sourceId}_$pathHash"
    }

    suspend fun loadPrefs(sourceId: String, path: String): FolderPrefs {
        val key = makeKey(sourceId, path)
        val prefsKey = stringPreferencesKey(key)

        val prefs = context.dataStore.data.first()
        val json = prefs[prefsKey] ?: return FolderPrefs()

        return parsePrefs(json)
    }

    suspend fun savePrefs(sourceId: String, path: String, prefs: FolderPrefs) {
        val key = makeKey(sourceId, path)
        val prefsKey = stringPreferencesKey(key)
        val timestampKey = longPreferencesKey("${key}_ts")

        context.dataStore.edit { store ->
            store[prefsKey] = serializePrefs(prefs)
            store[timestampKey] = System.currentTimeMillis()
        }

        cleanupIfNeeded()
    }

    private fun serializePrefs(prefs: FolderPrefs): String {
        return "${prefs.viewMode.name}$SEPARATOR${prefs.sortMode.name}"
    }

    private fun parsePrefs(json: String): FolderPrefs {
        val parts = json.split(SEPARATOR)
        if (parts.size != 2) return FolderPrefs()

        return try {
            FolderPrefs(
                viewMode = FileViewMode.valueOf(parts[0]),
                sortMode = FileSortMode.valueOf(parts[1])
            )
        } catch (e: IllegalArgumentException) {
            FolderPrefs()
        }
    }

    private suspend fun cleanupIfNeeded() {
        val allPrefs = context.dataStore.data.first()
        val fbKeys = allPrefs.asMap().keys.filter { key ->
            key.name.startsWith(KEY_PREFIX) && !key.name.endsWith("_ts")
        }

        if (fbKeys.size <= MAX_RECORDS) return

        val timestampKeys = allPrefs.asMap().keys.filter { key ->
            key.name.startsWith(KEY_PREFIX) && key.name.endsWith("_ts")
        }

        val timestamps = timestampKeys.mapNotNull { key ->
            val ts = allPrefs[key] as? Long ?: return@mapNotNull null
            key.name.removeSuffix("_ts") to ts
        }.sortedBy { it.second }

        val toRemove = timestamps.take(fbKeys.size - MAX_RECORDS)

        context.dataStore.edit { store ->
            toRemove.forEach { (keyBase, _) ->
                store.remove(stringPreferencesKey(keyBase))
                store.remove(longPreferencesKey("${keyBase}_ts"))
            }
        }
    }
}
