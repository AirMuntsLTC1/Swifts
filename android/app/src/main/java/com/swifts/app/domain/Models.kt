package com.swifts.app.domain

import kotlinx.serialization.Serializable

/** Storage bucket that holds post images. Paths are stored as "<user_id>/<file>". */
const val POST_MEDIA_BUCKET = "posts"

/** Storage bucket for profile photos. Paths are stored as "<user_id>/<file>". */
const val AVATAR_BUCKET = "avatars"

@Serializable
data class Author(
    val username: String,
    val display_name: String = "",
    val avatar_url: String? = null,
)

@Serializable
data class PostMedia(
    val kind: String,
    val storage_path: String,
    val thumb_path: String? = null,
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
    val post_media: List<PostMedia> = emptyList(),
)

@Serializable
data class FeedPage(
    val items: List<Post>,
    val next_cursor: String? = null,
)

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val display_name: String = "",
    val avatar_url: String? = null,
    val bio: String = "",
    val city: String? = null,
    val hometown: String? = null,
    val work: String? = null,
    val school: String? = null,
)

@Serializable
data class ProfileUpdate(
    val display_name: String,
    val bio: String,
    val city: String?,
    val hometown: String?,
    val work: String?,
    val school: String?,
)

