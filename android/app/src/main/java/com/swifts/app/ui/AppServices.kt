package com.swifts.app.ui

import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.CommentRepository
import com.swifts.app.domain.GroupRepository
import com.swifts.app.domain.MediaStorage
import com.swifts.app.domain.MessageRepository
import com.swifts.app.domain.NotificationRepository
import com.swifts.app.domain.PostRepository
import com.swifts.app.domain.ProfileRepository
import com.swifts.app.domain.StoryRepository

/** Everything the screens need, built once in MainActivity. */
data class AppServices(
    val auth: AuthRepository,
    val posts: PostRepository,
    val profiles: ProfileRepository,
    val groups: GroupRepository,
    val stories: StoryRepository,
    val messages: MessageRepository,
    val notifications: NotificationRepository,
    val comments: CommentRepository,
    val storage: MediaStorage,
    val lang: String,
)
