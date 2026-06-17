package com.safenet.vpn.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token manager utilizing Jetpack Security's EncryptedSharedPreferences.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_FILE = "safenet_secure_tokens"
        private const val KEY_ACCESS = "key_access_token"
        private const val KEY_REFRESH = "key_refresh_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_DEVICE_ID = "key_device_id"
        private const val KEY_DEVICE_SECRET = "key_device_secret"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_USED_TRAFFIC = "key_used_traffic"
        private const val KEY_LAST_NOTIFICATION_SYNC = "key_last_notification_sync"
        private const val KEY_SELECTED_SERVER_ID = "key_selected_server_id"
        // Legacy single-key fields (kept for migration path)
        private const val KEY_EXTERNAL_AMNEZIA_CONFIG = "key_external_amnezia_config"
        private const val KEY_EXTERNAL_AMNEZIA_NAME = "key_external_amnezia_name"
        private const val KEY_EXTERNAL_AMNEZIA_TYPE = "key_external_amnezia_type"
        // Multi-key config prefix (keyed by UUID)
        private const val PREFIX_EXT_KEY_CONFIG = "ext_key_cfg_"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPrefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = sharedPrefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = sharedPrefs.getString(KEY_REFRESH, null)

    fun saveUserId(userId: String) {
        sharedPrefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    fun getUserId(): String? = sharedPrefs.getString(KEY_USER_ID, null)

    fun saveUsername(username: String) {
        sharedPrefs.edit().putString(KEY_USERNAME, username).apply()
    }
    fun getUsername(): String? = sharedPrefs.getString(KEY_USERNAME, null)

    fun saveUsedTraffic(bytes: Long) {
        sharedPrefs.edit().putLong(KEY_USED_TRAFFIC, bytes).apply()
    }
    fun getUsedTraffic(): Long = sharedPrefs.getLong(KEY_USED_TRAFFIC, 0L)

    fun saveDeviceId(deviceId: String) {
        sharedPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }
    fun getDeviceId(): String? = sharedPrefs.getString(KEY_DEVICE_ID, null)

    fun saveDeviceSecret(deviceSecret: String) {
        sharedPrefs.edit().putString(KEY_DEVICE_SECRET, deviceSecret).apply()
    }
    fun getDeviceSecret(): String? = sharedPrefs.getString(KEY_DEVICE_SECRET, null)

    fun saveLastNotificationSync(createdAt: String) {
        sharedPrefs.edit().putString(KEY_LAST_NOTIFICATION_SYNC, createdAt).apply()
    }
    fun getLastNotificationSync(): String? = sharedPrefs.getString(KEY_LAST_NOTIFICATION_SYNC, null)

    fun saveSelectedServerId(serverId: String) {
        sharedPrefs.edit().putString(KEY_SELECTED_SERVER_ID, serverId).apply()
    }
    fun getSelectedServerId(): String? = sharedPrefs.getString(KEY_SELECTED_SERVER_ID, null)

    // ── Legacy single-key methods (kept for migration, do not use for new code) ─────────
    fun saveExternalAmneziaConfig(config: String, name: String, type: String) {
        sharedPrefs.edit()
            .putString(KEY_EXTERNAL_AMNEZIA_CONFIG, config)
            .putString(KEY_EXTERNAL_AMNEZIA_NAME, name)
            .putString(KEY_EXTERNAL_AMNEZIA_TYPE, type)
            .apply()
    }

    fun getExternalAmneziaConfig(): String? = sharedPrefs.getString(KEY_EXTERNAL_AMNEZIA_CONFIG, null)
    fun getExternalAmneziaName(): String? = sharedPrefs.getString(KEY_EXTERNAL_AMNEZIA_NAME, null)
    fun getExternalAmneziaType(): String? = sharedPrefs.getString(KEY_EXTERNAL_AMNEZIA_TYPE, null)

    fun clearExternalAmneziaConfig() {
        sharedPrefs.edit()
            .remove(KEY_EXTERNAL_AMNEZIA_CONFIG)
            .remove(KEY_EXTERNAL_AMNEZIA_NAME)
            .remove(KEY_EXTERNAL_AMNEZIA_TYPE)
            .apply()
    }

    // ── Multi-key config storage (keyed by UUID) ─────────────────────────────────────────
    /** Save raw AmneziaWG config text encrypted under the given key [id]. */
    fun saveExternalKeyConfig(id: String, config: String) {
        sharedPrefs.edit().putString(PREFIX_EXT_KEY_CONFIG + id, config).apply()
    }

    /** Retrieve raw AmneziaWG config text for [id]. Returns null if not found. */
    fun getExternalKeyConfig(id: String): String? =
        sharedPrefs.getString(PREFIX_EXT_KEY_CONFIG + id, null)

    /** Remove the config text for [id] (called on key deletion). */
    fun removeExternalKeyConfig(id: String) {
        sharedPrefs.edit().remove(PREFIX_EXT_KEY_CONFIG + id).apply()
    }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}
