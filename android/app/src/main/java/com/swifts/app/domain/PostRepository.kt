package com.swifts.app.domain

interface PostRepository {
    suspend fun feed(lang: String, cursor: String? = null, limit: Int = 20): Result<FeedPage>
    suspend fun followingFeed(authorIds: List<String>, limit: Int = 20): Result<FeedPage>
    suspend fun postsBy(authorId: String, limit: Int = 30): Result<List<Post>>
    suspend fun postsByGroup(groupId: String, limit: Int = 30): Result<List<Post>>
    suspend fun createPost(authorId: String, body: String, lang: String, groupId: String? = null): Result<Post>
    suspend fun updatePost(postId: String, body: String): Result<Post>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun setLiked(userId: String, postId: String, liked: Boolean): Result<Unit>
}
