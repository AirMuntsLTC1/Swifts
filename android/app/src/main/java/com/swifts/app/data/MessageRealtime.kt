package com.swifts.app.data

import com.swifts.app.domain.Message
import com.swifts.app.domain.Subscription
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Listens for new rows in one chat over the Supabase Realtime websocket.
 * Reconnects with backoff if the connection drops, and sends heartbeats so it stays open.
 */
class MessageRealtime(
    private val api: SupabaseClient,
    private val conversationId: String,
    private val onMessage: (Message) -> Unit,
) : Subscription {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var socket: WebSocket? = null
    private var ref = 0

    fun start(): Subscription {
        scope.launch {
            var backoffMs = 1_000L
            while (isActive) {
                val token = api.accessToken() ?: break
                val closed = CompletableDeferred<Unit>()
                val url = api.base.replaceFirst("https://", "wss://") +
                    "/realtime/v1/websocket?apikey=${api.anon}&vsn=1.0.0"

                socket = api.http.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.send(envelope(topic(), "phx_join", joinPayload(token)))
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handle(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        closed.complete(Unit)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        closed.complete(Unit)
                    }
                })

                val heartbeat = launch {
                    while (isActive) {
                        delay(HEARTBEAT_MS)
                        socket?.send(envelope("phoenix", "heartbeat", JsonObject(emptyMap())))
                    }
                }

                closed.await()
                heartbeat.cancel()
                socket = null
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
        return this
    }

    override fun cancel() {
        socket?.close(1000, "closed")
        socket = null
        scope.cancel()
    }

    private fun topic() = "realtime:chat-$conversationId"

    private fun joinPayload(token: String): JsonObject = buildJsonObject {
        putJsonObject("config") {
            putJsonArray("postgres_changes") {
                addJsonObject {
                    put("event", "INSERT")
                    put("schema", "public")
                    put("table", "messages")
                    put("filter", "conversation_id=eq.$conversationId")
                }
            }
        }
        put("access_token", token)
    }

    private fun envelope(topic: String, event: String, payload: JsonObject): String {
        ref += 1
        return buildJsonObject {
            put("topic", topic)
            put("event", event)
            put("payload", payload)
            put("ref", ref.toString())
        }.toString()
    }

    private fun handle(text: String) {
        runCatching {
            val root = api.json.parseToJsonElement(text).jsonObject
            if (root["event"]?.jsonPrimitive?.content != "postgres_changes") return
            val data = root["payload"]?.jsonObject?.get("data")?.jsonObject ?: return
            val record = data["record"] ?: data["new"] ?: return
            val message = api.json.decodeFromJsonElement(Message.serializer(), record)
            if (message.conversation_id == conversationId) onMessage(message)
        }
    }

    private companion object {
        const val HEARTBEAT_MS = 25_000L
        const val MAX_BACKOFF_MS = 30_000L
    }
}
