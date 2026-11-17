package com.swifts.app.ui

import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.GroupRepository
import com.swifts.app.domain.PostRepository
import com.swifts.app.domain.ProfileRepository

/** Everything the screens need, built once in MainActivity. */
data class AppServices(
    val auth: AuthRepository,
    val posts: PostRepository,
    val profiles: ProfileRepository,
    val groups: GroupRepository,
    val lang: String,
)
