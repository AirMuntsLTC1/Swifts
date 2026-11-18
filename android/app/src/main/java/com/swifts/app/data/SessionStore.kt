package com.swifts.app.data

import android.content.Context
import com.swifts.app.domain.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Light, dark, or follow the system. Stored on this device, so it survives sign-out. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("swifts_session", Context.MODE_PRIVATE)

    // Separate file on purpose: clear() on sign-out wipes swifts_session only, so the theme choice stays.
    private val settings = context.getSharedPreferences("swifts_settings", Context.MODE_PRIVATE)

    private val _signedIn = MutableStateFlow(load() != null)

    /** True while a session exists. When it turns false, the app shows the sign-in screen. */
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _themeMode = MutableStateFlow(readThemeMode())

    /** The user's theme choice. Changes apply immediately and are saved for next launch. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settings.edit().putString(K_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode {
        val saved = settings.getString(K_THEME, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun save(s: Session) {
        prefs.edit()
            .putString(K_ACCESS, s.accessToken)
            .putString(K_REFRESH, s.refreshToken)
            .putString(K_USER, s.userId)
            .putLong(K_EXP, s.expiresAtEpochSec)
            .apply()
        _signedIn.value = true
    }

    fun load(): Session? {
        val access = prefs.getString(K_ACCESS, null) ?: return null
        return Session(
            accessToken = access,
            refreshToken = prefs.getString(K_REFRESH, "") ?: "",
            userId = prefs.getString(K_USER, "") ?: "",
            expiresAtEpochSec = prefs.getLong(K_EXP, 0L),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
        _signedIn.value = false
    }

    private companion object {
        const val K_ACCESS = "access"
        const val K_REFRESH = "refresh"
        const val K_USER = "user"
        const val K_EXP = "exp"
        const val K_THEME = "theme_mode"
    }
}
