package com.swifts.app.data

import com.swifts.app.BuildConfig
import com.swifts.app.domain.Session
import kotlinx.coroutines.Dispatchers
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
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anon = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Valid access token, refreshed when close to expiry. Null when signed out. */
    suspend fun accessToken(): String? {
        val s = store.load() ?: return null
        if (s.expiresAtEpochSec - 60 > System.currentTimeMillis() / 1000) return s.accessToken
        val body = buildJsonObject { put("refresh_token", s.refreshToken) }.toString()
        return try {
            val fresh = parseSession(request("POST", "/auth/v1/token?grant_type=refresh_token", body, auth = false))
            store.save(fresh)
            fresh.accessToken
        } catch (e: SupabaseException) {
            store.clear()
            null
        }
    }

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
        return Session(
            accessToken = o.getValue("access_token").jsonPrimitive.content,
            refreshToken = o.getValue("refresh_token").jsonPrimitive.content,
            userId = o.getValue("user").jsonObject.getValue("id").jsonPrimitive.content,
            expiresAtEpochSec = o["expires_at"]?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }

    private fun extractMessage(text: String): String? = runCatching {
        val o = json.parseToJsonElement(text).jsonObject
        listOf("msg", "error_description", "message", "error")
            .firstNotNullOfOrNull { o[it]?.jsonPrimitive?.contentOrNull }
    }.getOrNull()
}
