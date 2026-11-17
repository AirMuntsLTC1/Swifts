package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import com.swifts.app.domain.Profile
import com.swifts.app.domain.ProfileRepository
import com.swifts.app.domain.ProfileUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: Profile? = null,
    val isMe: Boolean = false,
    val following: Boolean = false,
    val posts: List<Post> = emptyList(),
    val editing: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    private val profiles: ProfileRepository,
    private val posts: PostRepository,
    private val auth: AuthRepository,
    private val userId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState(isMe = userId == auth.currentUserId()))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val profile = profiles.profile(userId).getOrElse { e ->
                _state.update { it.copy(loading = false, error = e.message) }
                return@launch
            }
            val feed = posts.postsBy(userId).getOrElse { emptyList() }
            val me = auth.currentUserId()
            val following = if (me == null || me == userId) {
                false
            } else {
                profiles.followingIds(me).getOrElse { emptySet() }.contains(userId)
            }
            _state.update {
                it.copy(loading = false, profile = profile, posts = feed, following = following)
            }
        }
    }

    fun toggleFollow() {
        val me = auth.currentUserId() ?: return
        val nowFollowing = !_state.value.following
        _state.update { it.copy(following = nowFollowing) }
        viewModelScope.launch {
            profiles.setFollowing(me, userId, nowFollowing).onFailure {
                _state.update { it.copy(following = !nowFollowing) }
            }
        }
    }

    fun setEditing(on: Boolean) {
        _state.update { it.copy(editing = on) }
    }

    fun save(update: ProfileUpdate) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            profiles.update(userId, update).fold(
                onSuccess = { p -> _state.update { it.copy(saving = false, editing = false, profile = p) } },
                onFailure = { e -> _state.update { it.copy(saving = false, error = e.message) } },
            )
        }
    }
}
