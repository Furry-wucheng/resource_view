package dev.wucheng.resource_viewer.data.local.secure

import android.content.SharedPreferences

/**
 * 安全偏好设置封装类。
 * 使用 EncryptedSharedPreferences 存储敏感数据（如 SMB 密码）。
 *
 * 注意：此实现遵循 doc/share/03-di-contracts.md 中的 SecurePrefsModule 契约。
 */
class SecurePrefs(val prefs: SharedPreferences) {

    companion object {
        private const val KEY_PREFIX_PASSWORD = "password_"
    }

    /**
     * 存储指定数据源的密码。
     * @param sourceId 数据源 ID
     * @param password 密码
     */
    fun putPassword(sourceId: String, password: String) {
        prefs.edit()
            .putString(KEY_PREFIX_PASSWORD + sourceId, password)
            .apply()
    }

    /**
     * 获取指定数据源的密码。
     * @param sourceId 数据源 ID
     * @return 密码，如果不存在则返回 null
     */
    fun getPassword(sourceId: String): String? {
        return prefs.getString(KEY_PREFIX_PASSWORD + sourceId, null)
    }

    /**
     * 移除指定数据源的密码。
     * @param sourceId 数据源 ID
     */
    fun removePassword(sourceId: String) {
        prefs.edit()
            .remove(KEY_PREFIX_PASSWORD + sourceId)
            .apply()
    }
}
