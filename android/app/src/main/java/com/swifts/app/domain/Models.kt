package com.swifts.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val author_id: String,
    val body: String,
    val lang: String,
    val created_at: String,
)

@Serializable
data class FeedPage(
    val items: List<Post>,
    val next_cursor: String? = null,
)
