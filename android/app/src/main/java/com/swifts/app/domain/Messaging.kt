package com.swifts.app.domain

import kotlinx.serialization.Serializable

data class ConversationSummary(
    val id: String,
    val otherId: String,
    val username: String,
    val name: String,
    val avatarUrl: String?,
)

@Serializable
data class Message(
    val id: String,
    val conversation_id: String,
    val sender_id: String,
    val body: String,
    val created_at: String,
)

interface MessageRepository {
    suspend fun conversations(userId: String): Result<List<ConversationSummary>>
    suspend fun startConversation(otherUserId: String): Result<String>
    suspend fun messages(conversationId: String): Result<List<Message>>
    suspend fun send(conversationId: String, senderId: String, body: String): Result<Message>
}
