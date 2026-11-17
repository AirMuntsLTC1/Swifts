package com.swifts.app.domain

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Session>
    /** Returns null when the project requires email confirmation before sign-in. */
    suspend fun signUp(email: String, password: String, username: String, displayName: String): Result<Session?>
    fun signOut(): Result<Unit>
    fun currentUserId(): String?
}
