package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Message
import com.swifts.app.domain.MessageRepository
import com.swifts.app.domain.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val error: String? = null,
)

/** Loads the history once, then adds new messages as they arrive over Realtime. */
class ChatViewModel(
    private val messages: MessageRepository,
    private val auth: AuthRepository,
    private val conversationId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val subscription: Subscription = messages.subscribe(conversationId) { m ->
        _state.update { s -> s.copy(messages = merge(s.messages, listOf(m))) }
    }

    init {
        viewModelScope.launch {
            messages.messages(conversationId).onSuccess { list ->
                _state.update { s -> s.copy(messages = merge(s.messages, list)) }
            }
        }
    }

    fun send(text: String) {
        val uid = auth.currentUserId() ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            messages.send(conversationId, uid, body).fold(
                onSuccess = { m -> _state.update { s -> s.copy(messages = merge(s.messages, listOf(m))) } },
                onFailure = { e -> _state.update { it.copy(error = e.message) } },
            )
        }
    }

    override fun onCleared() {
        subscription.cancel()
    }

    /** Combines lists without duplicates, in time order. */
    private fun merge(current: List<Message>, incoming: List<Message>): List<Message> =
        (current + incoming).distinctBy { it.id }.sortedBy { it.created_at }
}
