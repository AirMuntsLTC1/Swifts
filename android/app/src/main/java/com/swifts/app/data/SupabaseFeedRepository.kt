package com.swifts.app.data

import com.swifts.app.BuildConfig
import com.swifts.app.domain.FeedPage
import com.swifts.app.domain.PostRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class SupabaseFeedRepository(
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val accessToken: () -> String?,
) : PostRepository {

    override suspend fun feed(lang: String, cursor: String?, limit: Int): Result<FeedPage> =
        runCatching {
            val base = "${BuildConfig.SUPABASE_URL}/functions/v1/feed"
            val q = buildString {
                append("?lang=").append(enc(lang))
                append("&limit=").append(limit)
                if (cursor != null) append("&cursor=").append(enc(cursor))
            }
            val req = Request.Builder()
                .url(base + q)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${accessToken() ?: BuildConfig.SUPABASE_ANON_KEY}")
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("feed failed ${resp.code}: $body")
                json.decodeFromString(FeedPage.serializer(), body)
            }
        }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
