package com.swifts.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    val post_id: String,
    val parent_id: String? = null,
    val author_id: String,
    val body: String,
    val created_at: String,
    val author: Author? = null,
)

@Serializable
data class CommentLike(
    val comment_id: String,
    val user_id: String,
)

interface CommentRepository {
    suspend fun list(postId: String): Result<List<Comment>>
    suspend fun likes(commentIds: List<String>): Result<List<CommentLike>>
    suspend fun add(postId: String, authorId: String, body: String, parentId: String?): Result<Comment>
    suspend fun setLiked(commentId: String, userId: String, liked: Boolean): Result<Unit>
    suspend fun delete(commentId: String): Result<Unit>
}
