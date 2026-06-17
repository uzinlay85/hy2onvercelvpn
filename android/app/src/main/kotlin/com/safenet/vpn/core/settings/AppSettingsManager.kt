package com.safenet.vpn.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String?): ThemeMode =
            entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

@Singleton
class AppSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_FILE = "safenet_app_settings"
        const val ACTION_SETTINGS_CHANGED = "com.safenet.vpn.SETTINGS_CHANGED"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_CONNECT_BOOT = "auto_connect_boot"
    }

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = if (prefs.contains(KEY_THEME_MODE)) {
            ThemeMode.fromValue(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.value))
        } else {
            if (prefs.getBoolean(KEY_DARK_MODE, false)) ThemeMode.DARK else ThemeMode.SYSTEM
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.value).apply()
            notifyChanged()
        }

    var isDarkMode: Boolean
        get() = themeMode == ThemeMode.DARK
        set(value) {
            themeMode = if (value) ThemeMode.DARK else ThemeMode.LIGHT
        }

    var autoConnectOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_BOOT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_CONNECT_BOOT, value).apply()
            notifyChanged()
        }

    private fun notifyChanged() {
        context.sendBroadcast(android.content.Intent(ACTION_SETTINGS_CHANGED).setPackage(context.packageName))
    }
}
