package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.ConversationSummary
import com.swifts.app.domain.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessagesUiState(
    val loading: Boolean = false,
    val items: List<ConversationSummary> = emptyList(),
    val error: String? = null,
)

class MessagesViewModel(
    private val messages: MessageRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MessagesUiState())
    val state: StateFlow<MessagesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val uid = auth.currentUserId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            messages.conversations(uid).fold(
                onSuccess = { list -> _state.update { it.copy(loading = false, items = list) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }
}
