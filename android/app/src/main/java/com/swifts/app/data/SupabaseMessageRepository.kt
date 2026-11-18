package com.swifts.app.data

import com.swifts.app.domain.Author
import com.swifts.app.domain.ConversationSummary
import com.swifts.app.domain.Message
import com.swifts.app.domain.MessageRepository
import com.swifts.app.domain.Subscription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

class SupabaseMessageRepository(private val api: SupabaseClient) : MessageRepository {

    @Serializable
    private data class MemberRow(val conversation_id: String)

    @Serializable
    private data class OtherRow(
        val conversation_id: String,
        val user_id: String,
        val profile: Author? = null,
    )

    override suspend fun conversations(userId: String): Result<List<ConversationSummary>> = runCatching {
        val mine = api.json.decodeFromString<List<MemberRow>>(
            api.request("GET", "/rest/v1/conversation_members?select=conversation_id&user_id=eq.${enc(userId)}")
        )
        if (mine.isEmpty()) return@runCatching emptyList<ConversationSummary>()

        val ids = mine.joinToString(",") { it.conversation_id }
        val others = api.json.decodeFromString<List<OtherRow>>(
            api.request(
                "GET",
                "/rest/v1/conversation_members?select=conversation_id,user_id," +
                    "profile:profiles!conversation_members_user_id_fkey(username,display_name,avatar_url)" +
                    "&conversation_id=in.($ids)&user_id=neq.${enc(userId)}",
            )
        )
        others.map { o ->
            val p = o.profile
            ConversationSummary(
                id = o.conversation_id,
                otherId = o.user_id,
                username = p?.username ?: "user",
                name = p?.display_name?.takeIf { it.isNotBlank() } ?: p?.username ?: "",
                avatarUrl = p?.avatar_url,
            )
        }
    }

    override suspend fun startConversation(otherUserId: String): Result<String> = runCatching {
        val body = buildJsonObject { put("other", otherUserId) }.toString()
        api.json.decodeFromString<String>(api.request("POST", "/rest/v1/rpc/start_conversation", body))
    }

    override suspend fun messages(conversationId: String): Result<List<Message>> = runCatching {
        api.json.decodeFromString<List<Message>>(
            api.request(
                "GET",
                "/rest/v1/messages?select=id,conversation_id,sender_id,body,created_at" +
                    "&conversation_id=eq.${enc(conversationId)}&order=created_at.asc&limit=200",
            )
        )
    }

    override suspend fun send(conversationId: String, senderId: String, body: String): Result<Message> = runCatching {
        val payload = buildJsonObject {
            put("conversation_id", conversationId)
            put("sender_id", senderId)
            put("body", body)
        }.toString()
        api.json.decodeFromString<List<Message>>(
            api.request(
                "POST",
                "/rest/v1/messages?select=id,conversation_id,sender_id,body,created_at",
                payload,
                prefer = "return=representation",
            )
        ).first()
    }

    override fun subscribe(conversationId: String, onMessage: (Message) -> Unit): Subscription =
        MessageRealtime(api, conversationId, onMessage).start()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
