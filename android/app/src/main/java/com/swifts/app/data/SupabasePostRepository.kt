package com.swifts.app.data

import com.swifts.app.domain.FeedPage
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

class SupabasePostRepository(private val api: SupabaseClient) : PostRepository {

    override suspend fun feed(lang: String, cursor: String?, limit: Int): Result<FeedPage> = runCatching {
        val query = buildString {
            append("?lang=").append(enc(lang)).append("&limit=").append(limit)
            if (cursor != null) append("&cursor=").append(enc(cursor))
        }
        api.json.decodeFromString(FeedPage.serializer(), api.request("GET", "/functions/v1/feed$query"))
    }

    override suspend fun createPost(authorId: String, body: String, lang: String): Result<Post> = runCatching {
        val payload = buildJsonObject {
            put("author_id", authorId)
            put("body", body)
            put("lang", lang)
        }.toString()
        val text = api.request(
            "POST",
            "/rest/v1/posts?select=id,author_id,body,lang,created_at",
            payload,
            prefer = "return=representation",
        )
        api.json.decodeFromString<List<Post>>(text).first()
    }

    override suspend fun setLiked(userId: String, postId: String, liked: Boolean): Result<Unit> = runCatching {
        if (liked) {
            val payload = buildJsonObject { put("user_id", userId); put("post_id", postId) }.toString()
            api.request("POST", "/rest/v1/likes", payload, prefer = "resolution=ignore-duplicates,return=minimal")
        } else {
            api.request("DELETE", "/rest/v1/likes?user_id=eq.$userId&post_id=eq.$postId")
        }
        Unit
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
