package com.swifts.app.data

import android.app.Activity
import com.swifts.app.BuildConfig
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Session
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

class SupabaseAuthRepository(
    private val api: SupabaseClient,
    private val store: SessionStore,
) : AuthRepository {

    override suspend fun signInWithGoogle(activity: Activity): Result<Session> = runCatching {
        // Raw nonce goes to Supabase; its SHA-256 goes to Google. Supabase checks they match.
        val rawNonce = UUID.randomUUID().toString()
        val idToken = GoogleCredentialSource.fetchIdToken(
            activity = activity,
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            hashedNonce = sha256Hex(rawNonce),
        )
        val body = buildJsonObject {
            put("provider", "google")
            put("id_token", idToken)
            put("nonce", rawNonce)
        }.toString()
        val session = api.parseSession(
            api.request("POST", "/auth/v1/token?grant_type=id_token", body, auth = false)
        )
        store.save(session)
        session
    }

    override fun signOut(): Result<Unit> = runCatching { store.clear() }

    override fun currentUserId(): String? = store.load()?.userId

    private fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
