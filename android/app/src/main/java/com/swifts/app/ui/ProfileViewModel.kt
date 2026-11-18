package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.data.PickedMedia
import com.swifts.app.domain.AVATAR_BUCKET
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.MediaStorage
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import com.swifts.app.domain.Profile
import com.swifts.app.domain.ProfileRepository
import com.swifts.app.domain.ProfileUpdate
import java.util.UUID
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
    val uploadingAvatar: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    private val profiles: ProfileRepository,
    private val posts: PostRepository,
    private val storage: MediaStorage,
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

    /** Uploads a new profile photo to the avatars bucket, then saves its URL on your profile. Only your own profile can change. */
    fun uploadAvatar(media: PickedMedia) {
        val uid = auth.currentUserId() ?: return
        if (uid != userId) return
        // A new random file name each time, so the image cache never shows an old photo.
        val path = "$uid/${UUID.randomUUID()}.${media.ext}"
        viewModelScope.launch {
            _state.update { it.copy(uploadingAvatar = true, error = null) }
            val uploaded = storage.upload(AVATAR_BUCKET, path, media.bytes, media.mime)
            if (uploaded.isFailure) {
                _state.update { it.copy(uploadingAvatar = false, error = uploaded.exceptionOrNull()?.message) }
                return@launch
            }
            profiles.setAvatar(userId, storage.publicUrl(AVATAR_BUCKET, path)).fold(
                onSuccess = { p -> _state.update { it.copy(uploadingAvatar = false, profile = p) } },
                onFailure = { e -> _state.update { it.copy(uploadingAvatar = false, error = e.message) } },
            )
        }
    }
}
