package com.swifts.app.data

import com.swifts.app.domain.AppNotification
import com.swifts.app.domain.NotificationRepository
import java.net.URLEncoder

class SupabaseNotificationRepository(private val api: SupabaseClient) : NotificationRepository {

    override suspend fun list(userId: String): Result<List<AppNotification>> = runCatching {
        api.json.decodeFromString<List<AppNotification>>(
            api.request(
                "GET",
                "/rest/v1/notifications?select=id,actor_id,kind,ref_id,read,created_at," +
                    "actor:profiles!notifications_actor_id_fkey(username,display_name,avatar_url)" +
                    "&user_id=eq.${enc(userId)}&order=created_at.desc&limit=50",
            )
        )
    }

    override suspend fun markAllRead(userId: String): Result<Unit> = runCatching {
        api.request(
            "PATCH",
            "/rest/v1/notifications?user_id=eq.${enc(userId)}&read=eq.false",
            """{"read":true}""",
            prefer = "return=minimal",
        )
        Unit
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
