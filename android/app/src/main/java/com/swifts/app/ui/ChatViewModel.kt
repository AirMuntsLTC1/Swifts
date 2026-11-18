package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Message
import com.swifts.app.domain.MessageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val error: String? = null,
)

/** Loads messages and refreshes them every few seconds while the chat is open. */
class ChatViewModel(
    private val messages: MessageRepository,
    private val auth: AuthRepository,
    private val conversationId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(POLL_MS)
            }
        }
    }

    private suspend fun refresh() {
        messages.messages(conversationId).onSuccess { list ->
            _state.update { it.copy(messages = list) }
        }
    }

    fun send(text: String) {
        val uid = auth.currentUserId() ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            messages.send(conversationId, uid, body).fold(
                onSuccess = { m -> _state.update { it.copy(messages = it.messages + m) } },
                onFailure = { e -> _state.update { it.copy(error = e.message) } },
            )
        }
    }

    private companion object {
        const val POLL_MS = 3000L
    }
}
