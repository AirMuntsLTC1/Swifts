package com.swifts.app.domain

import android.app.Activity

interface AuthRepository {
    /** Shows the native Google account chooser and signs the user in. */
    suspend fun signInWithGoogle(activity: Activity): Result<Session>
    fun signOut(): Result<Unit>
    fun currentUserId(): String?
}
