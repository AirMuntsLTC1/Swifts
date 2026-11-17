package com.swifts.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val name: String,
    val description: String = "",
    val privacy: String = "public",
    val cover_url: String? = null,
    val created_by: String,
)
