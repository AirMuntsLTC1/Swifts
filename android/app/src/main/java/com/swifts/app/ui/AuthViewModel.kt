package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val usernameInvalid: Boolean = false,
    val needsConfirm: Boolean = false,
    val signedIn: Boolean = false,
)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.signIn(email.trim(), password).fold(
                onSuccess = { _state.update { it.copy(loading = false, signedIn = true) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    fun signUp(email: String, password: String, username: String, displayName: String) {
        val handle = username.trim().lowercase()
        if (!USERNAME.matches(handle)) {
            _state.update { it.copy(usernameInvalid = true, error = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, usernameInvalid = false) }
            auth.signUp(email.trim(), password, handle, displayName.trim()).fold(
                onSuccess = { session ->
                    _state.update {
                        if (session == null) it.copy(loading = false, needsConfirm = true)
                        else it.copy(loading = false, signedIn = true)
                    }
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    private companion object {
        val USERNAME = Regex("^[a-z0-9_.]{3,30}$")
    }
}
