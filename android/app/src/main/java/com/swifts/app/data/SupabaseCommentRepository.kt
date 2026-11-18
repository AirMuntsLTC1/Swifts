package com.swifts.app.data

import com.swifts.app.domain.Comment
import com.swifts.app.domain.CommentLike
import com.swifts.app.domain.CommentRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

private const val COMMENT_SELECT =
    "id,post_id,parent_id,author_id,body,created_at,author:profiles!comments_author_id_fkey(username,display_name,avatar_url)"

class SupabaseCommentRepository(private val api: SupabaseClient) : CommentRepository {

    override suspend fun list(postId: String): Result<List<Comment>> = runCatching {
        api.json.decodeFromString<List<Comment>>(
            api.request("GET", "/rest/v1/comments?select=$COMMENT_SELECT&post_id=eq.${enc(postId)}&order=created_at.asc&limit=500")
        )
    }

    override suspend fun likes(commentIds: List<String>): Result<List<CommentLike>> = runCatching {
        if (commentIds.isEmpty()) return@runCatching emptyList<CommentLike>()
        val ids = commentIds.joinToString(",") { enc(it) }
        api.json.decodeFromString<List<CommentLike>>(
            api.request("GET", "/rest/v1/comment_likes?select=comment_id,user_id&comment_id=in.($ids)")
        )
    }

    override suspend fun add(postId: String, authorId: String, body: String, parentId: String?): Result<Comment> =
        runCatching {
            val payload = buildJsonObject {
                put("post_id", postId)
                put("author_id", authorId)
                put("body", body)
                if (parentId != null) put("parent_id", parentId)
            }.toString()
            api.json.decodeFromString<List<Comment>>(
                api.request(
                    "POST",
                    "/rest/v1/comments?select=$COMMENT_SELECT",
                    payload,
                    prefer = "return=representation",
                )
            ).first()
        }

    override suspend fun setLiked(commentId: String, userId: String, liked: Boolean): Result<Unit> = runCatching {
        if (liked) {
            val payload = buildJsonObject { put("comment_id", commentId); put("user_id", userId) }.toString()
            api.request("POST", "/rest/v1/comment_likes", payload, prefer = "resolution=ignore-duplicates,return=minimal")
        } else {
            api.request("DELETE", "/rest/v1/comment_likes?comment_id=eq.${enc(commentId)}&user_id=eq.${enc(userId)}")
        }
        Unit
    }

    override suspend fun delete(commentId: String): Result<Unit> = runCatching {
        api.request("DELETE", "/rest/v1/comments?id=eq.${enc(commentId)}")
        Unit
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
