package com.swifts.app.ui

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
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
    val signedIn: Boolean = false,
)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.signInWithGoogle(activity).fold(
                onSuccess = { _state.update { it.copy(loading = false, signedIn = true) } },
                onFailure = { e ->
                    // The user closing the account chooser is not an error.
                    if (e is GetCredentialCancellationException) {
                        _state.update { it.copy(loading = false) }
                    } else {
                        _state.update { it.copy(loading = false, error = e.message ?: "Sign-in failed") }
                    }
                },
            )
        }
    }
}
