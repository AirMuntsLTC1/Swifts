package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AppNotification
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.NotificationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = false,
    val items: List<AppNotification> = emptyList(),
    val error: String? = null,
) {
    val unread: Int get() = items.count { !it.read }
}

class NotificationsViewModel(
    private val notifications: NotificationRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val uid = auth.currentUserId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            notifications.list(uid).fold(
                onSuccess = { list -> _state.update { it.copy(loading = false, items = list) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    /** Called when the notifications screen opens. Waits a moment so the "New" section stays visible. */
    fun markReadSoon() {
        val uid = auth.currentUserId() ?: return
        viewModelScope.launch {
            delay(READ_DELAY_MS)
            notifications.markAllRead(uid).onSuccess {
                _state.update { s -> s.copy(items = s.items.map { it.copy(read = true) }) }
            }
        }
    }

    private companion object {
        const val READ_DELAY_MS = 1_500L
    }
}
