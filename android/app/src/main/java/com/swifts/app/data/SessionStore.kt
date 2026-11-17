package com.swifts.app.data

import android.content.Context
import com.swifts.app.domain.Session

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("swifts_session", Context.MODE_PRIVATE)

    fun save(s: Session) {
        prefs.edit()
            .putString(K_ACCESS, s.accessToken)
            .putString(K_REFRESH, s.refreshToken)
            .putString(K_USER, s.userId)
            .putLong(K_EXP, s.expiresAtEpochSec)
            .apply()
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
    }

    private companion object {
        const val K_ACCESS = "access"
        const val K_REFRESH = "refresh"
        const val K_USER = "user"
        const val K_EXP = "exp"
    }
}
