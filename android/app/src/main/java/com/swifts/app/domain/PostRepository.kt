package com.swifts.app.domain

interface PostRepository {
    suspend fun feed(lang: String, cursor: String? = null, limit: Int = 20): Result<FeedPage>
}
