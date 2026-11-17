package com.swifts.app.data

import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Session
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val api: SupabaseClient,
    private val store: SessionStore,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<Session> = runCatching {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString()
        val session = api.parseSession(api.request("POST", "/auth/v1/token?grant_type=password", body, auth = false))
        store.save(session)
        session
    }

    override suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): Result<Session?> = runCatching {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            putJsonObject("data") {
                put("username", username)
                put("display_name", displayName)
            }
        }.toString()
        val text = api.request("POST", "/auth/v1/signup", body, auth = false)
        if (api.json.parseToJsonElement(text).jsonObject["access_token"] == null) {
            null
        } else {
            api.parseSession(text).also { store.save(it) }
        }
    }

    override fun signOut(): Result<Unit> = runCatching { store.clear() }

    override fun currentUserId(): String? = store.load()?.userId
}
