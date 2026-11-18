package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import com.swifts.app.domain.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = false,
    val posting: Boolean = false,
    val showFollowing: Boolean = false,
    val items: List<Post> = emptyList(),
    val liked: Set<String> = emptySet(),
    val error: String? = null,
)

class FeedViewModel(
    private val posts: PostRepository,
    private val profiles: ProfileRepository,
    private val auth: AuthRepository,
    private val lang: String,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init { load() }

    fun setShowFollowing(on: Boolean) {
        if (_state.value.showFollowing == on) return
        _state.update { it.copy(showFollowing = on) }
        load()
    }

    fun load() {
        val uid = auth.currentUserId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = if (_state.value.showFollowing) {
                val ids = profiles.followingIds(uid).getOrElse { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                    return@launch
                }
                posts.followingFeed((ids + uid).toList())
            } else {
                posts.feed(lang)
            }
            result.fold(
                onSuccess = { page -> _state.update { it.copy(loading = false, items = page.items) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    fun publish(text: String) {
        val uid = auth.currentUserId() ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(posting = true, error = null) }
            posts.createPost(uid, body, lang).fold(
                onSuccess = { post -> _state.update { it.copy(posting = false, items = listOf(post) + it.items) } },
                onFailure = { e -> _state.update { it.copy(posting = false, error = e.message) } },
            )
        }
    }

    /** The signed-in user's id, used to show edit and delete on your own posts. */
    val meId: String get() = auth.currentUserId().orEmpty()

    fun updatePost(postId: String, body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            posts.updatePost(postId, text).fold(
                onSuccess = { updated ->
                    _state.update { s -> s.copy(items = s.items.map { if (it.id == postId) updated else it }) }
                },
                onFailure = { e -> _state.update { it.copy(error = e.message) } },
            )
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            posts.deletePost(postId).fold(
                onSuccess = { _state.update { s -> s.copy(items = s.items.filterNot { it.id == postId }) } },
                onFailure = { e -> _state.update { it.copy(error = e.message) } },
            )
        }
    }

    fun toggleLike(postId: String) {
        val uid = auth.currentUserId() ?: return
        val nowLiked = postId !in _state.value.liked
        _state.update { it.copy(liked = if (nowLiked) it.liked + postId else it.liked - postId) }
        viewModelScope.launch {
            posts.setLiked(uid, postId, nowLiked).onFailure { e ->
                _state.update {
                    it.copy(
                        error = e.message,
                        liked = if (nowLiked) it.liked - postId else it.liked + postId,
                    )
                }
            }
        }
    }
}
