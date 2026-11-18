package com.swifts.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val actor_id: String? = null,
    val kind: String,
    val ref_id: String? = null,
    val read: Boolean = false,
    val created_at: String,
    val actor: Author? = null,
)

interface NotificationRepository {
    suspend fun list(userId: String): Result<List<AppNotification>>
    suspend fun markAllRead(userId: String): Result<Unit>
}
