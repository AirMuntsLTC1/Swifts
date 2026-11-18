package com.swifts.app.data

import com.swifts.app.domain.FeedPage
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

private const val POST_SELECT =
    "id,author_id,body,lang,visibility,created_at," +
    "author:profiles!posts_author_id_fkey(username,display_name,avatar_url),post_media(kind,storage_path,thumb_path)"

class SupabasePostRepository(private val api: SupabaseClient) : PostRepository {

    override suspend fun feed(lang: String, cursor: String?, limit: Int): Result<FeedPage> = runCatching {
        val query = buildString {
            append("?lang=").append(enc(lang)).append("&limit=").append(limit)
            if (cursor != null) append("&cursor=").append(enc(cursor))
        }
        api.json.decodeFromString(FeedPage.serializer(), api.request("GET", "/functions/v1/feed$query"))
    }

    override suspend fun followingFeed(authorIds: List<String>, limit: Int): Result<FeedPage> = runCatching {
        if (authorIds.isEmpty()) return@runCatching FeedPage(items = emptyList())
        val ids = authorIds.joinToString(",") { enc(it) }
        val text = api.request(
            "GET",
            "/rest/v1/posts?select=$POST_SELECT&author_id=in.($ids)&group_id=is.null&order=created_at.desc&limit=$limit",
        )
        FeedPage(items = api.json.decodeFromString<List<Post>>(text))
    }

    override suspend fun postsBy(authorId: String, limit: Int): Result<List<Post>> = runCatching {
        val text = api.request(
            "GET",
            "/rest/v1/posts?select=$POST_SELECT&author_id=eq.${enc(authorId)}&group_id=is.null&order=created_at.desc&limit=$limit",
        )
        api.json.decodeFromString<List<Post>>(text)
    }

    override suspend fun postsByGroup(groupId: String, limit: Int): Result<List<Post>> = runCatching {
        val text = api.request(
            "GET",
            "/rest/v1/posts?select=$POST_SELECT&group_id=eq.${enc(groupId)}&order=created_at.desc&limit=$limit",
        )
        api.json.decodeFromString<List<Post>>(text)
    }

    override suspend fun createPost(authorId: String, body: String, lang: String, groupId: String?): Result<Post> =
        runCatching {
            val payload = buildJsonObject {
                put("author_id", authorId)
                put("body", body)
                put("lang", lang)
                if (groupId != null) put("group_id", groupId)
            }.toString()
            val text = api.request(
                "POST",
                "/rest/v1/posts?select=id,author_id,body,lang,visibility,created_at",
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
            api.request("DELETE", "/rest/v1/likes?user_id=eq.${enc(userId)}&post_id=eq.${enc(postId)}")
        }
        Unit
    }

    override suspend fun updatePost(postId: String, body: String): Result<Post> = runCatching {
        val payload = buildJsonObject { put("body", body) }.toString()
        api.json.decodeFromString<List<Post>>(
            api.request(
                "PATCH",
                "/rest/v1/posts?id=eq.${enc(postId)}&select=$POST_SELECT",
                payload,
                prefer = "return=representation",
            )
        ).first()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        api.request("DELETE", "/rest/v1/posts?id=eq.${enc(postId)}")
        Unit
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}

