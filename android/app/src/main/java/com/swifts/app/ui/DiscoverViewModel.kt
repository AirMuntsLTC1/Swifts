package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Profile
import com.swifts.app.domain.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Profile> = emptyList(),
    val following: Set<String> = emptySet(),
    val error: String? = null,
)

class DiscoverViewModel(
    private val profiles: ProfileRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        auth.currentUserId()?.let { uid ->
            viewModelScope.launch {
                profiles.followingIds(uid).onSuccess { ids ->
                    _state.update { it.copy(following = ids) }
                }
            }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // wait for typing to pause
            if (q.isBlank()) {
                _state.update { it.copy(results = emptyList(), loading = false, error = null) }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            profiles.search(q).fold(
                onSuccess = { list -> _state.update { it.copy(loading = false, results = list) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    fun toggleFollow(target: Profile) {
        val uid = auth.currentUserId() ?: return
        val nowFollowing = target.id !in _state.value.following
        _state.update {
            it.copy(following = if (nowFollowing) it.following + target.id else it.following - target.id)
        }
        viewModelScope.launch {
            profiles.setFollowing(uid, target.id, nowFollowing).onFailure {
                _state.update {
                    it.copy(following = if (nowFollowing) it.following - target.id else it.following + target.id)
                }
            }
        }
    }
}
