package com.swifts.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val username: String,
    val display_name: String = "",
    val avatar_url: String? = null,
)

@Serializable
data class Post(
    val id: String,
    val author_id: String,
    val body: String,
    val lang: String,
    val visibility: String = "public",
    val created_at: String,
    val author: Author? = null,
)

@Serializable
data class FeedPage(
    val items: List<Post>,
    val next_cursor: String? = null,
)
