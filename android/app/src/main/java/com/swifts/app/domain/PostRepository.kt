package com.swifts.app.domain

interface PostRepository {
    suspend fun feed(lang: String, cursor: String? = null, limit: Int = 20): Result<FeedPage>
    suspend fun createPost(authorId: String, body: String, lang: String): Result<Post>
    suspend fun setLiked(userId: String, postId: String, liked: Boolean): Result<Unit>
}
