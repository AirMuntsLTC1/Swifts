package com.swifts.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Story(
    val id: String,
    val author_id: String,
    val media_path: String,
    val created_at: String,
    val author: Author? = null,
)

interface StoryRepository {
    suspend fun visibleStories(authorIds: List<String>): Result<List<Story>>
    suspend fun create(authorId: String, mediaPath: String): Result<Story>
}
