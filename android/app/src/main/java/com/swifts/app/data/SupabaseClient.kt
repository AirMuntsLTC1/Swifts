package com.swifts.app.data

import com.swifts.app.BuildConfig
import com.swifts.app.domain.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SupabaseException(val code: Int, message: String) : Exception(message)

/** Single HTTP gateway to Supabase (Auth, PostgREST, Edge Functions). */
class SupabaseClient(
    private val store: SessionStore,
    val http: OkHttpClient = OkHttpClient(),
    val json: Json = Json { ignoreUnknownKeys = true },
) {
    val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    val anon = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Only one refresh runs at a time. Refresh tokens are single-use, so parallel refreshes would log the user out. */
    private val refreshLock = Mutex()

    /**
     * Valid access token, refreshed when close to expiry. Null only when the session is really gone.
     * Parallel callers wait for the same refresh instead of starting their own.
     */
    suspend fun accessToken(): String? {
        val current = store.load() ?: return null
        if (isFresh(current)) return current.accessToken

        return refreshLock.withLock {
            // Another caller may have refreshed while this one was waiting.
            val latest = store.load() ?: return@withLock null
            if (isFresh(latest)) return@withLock latest.accessToken

            val body = buildJsonObject { put("refresh_token", latest.refreshToken) }.toString()
            try {
                val fresh = parseSession(
                    request("POST", "/auth/v1/token?grant_type=refresh_token", body, auth = false)
                )
                store.save(fresh)
                fresh.accessToken
            } catch (e: SupabaseException) {
                if (e.code == 400 || e.code == 401 || e.code == 403) {
                    // Supabase rejected the refresh token: the session really has ended.
                    store.clear()
                    null
                } else {
                    // Temporary problem (network or server). Keep the session and try again later.
                    latest.accessToken
                }
            }
        }
    }

    private fun isFresh(s: Session): Boolean = s.expiresAtEpochSec - 60 > System.currentTimeMillis() / 1000

    suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        prefer: String? = null,
        auth: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        val token = if (auth) accessToken() ?: anon else anon
        val builder = Request.Builder()
            .url(base + path)
            .header("apikey", anon)
            .header("Authorization", "Bearer $token")
        if (prefer != null) builder.header("Prefer", prefer)
        builder.method(method, body?.toRequestBody(jsonType))
        http.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw SupabaseException(resp.code, extractMessage(text) ?: "HTTP ${resp.code}")
            }
            text
        }
    }

    fun parseSession(text: String): Session {
        val o = json.parseToJsonElement(text).jsonObject
        val nowSec = System.currentTimeMillis() / 1000
        // expires_at is normally sent. Fall back to expires_in so the token is never treated as always expired.
        val expiresAt = o["expires_at"]?.jsonPrimitive?.longOrNull
            ?: (nowSec + (o["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L))
        return Session(
            accessToken = o.getValue("access_token").jsonPrimitive.content,
            refreshToken = o.getValue("refresh_token").jsonPrimitive.content,
            userId = o.getValue("user").jsonObject.getValue("id").jsonPrimitive.content,
            expiresAtEpochSec = expiresAt,
        )
    }

    private fun extractMessage(text: String): String? = runCatching {
        val o = json.parseToJsonElement(text).jsonObject
        listOf("msg", "error_description", "message", "error")
            .firstNotNullOfOrNull { o[it]?.jsonPrimitive?.contentOrNull }
    }.getOrNull()
}
