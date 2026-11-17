package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = false,
    val posting: Boolean = false,
    val items: List<Post> = emptyList(),
    val liked: Set<String> = emptySet(),
    val error: String? = null,
)

class FeedViewModel(
    private val repo: PostRepository,
    private val auth: AuthRepository,
    private val lang: String,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.feed(lang).fold(
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
            repo.createPost(uid, body, lang).fold(
                onSuccess = { post -> _state.update { it.copy(posting = false, items = listOf(post) + it.items) } },
                onFailure = { e -> _state.update { it.copy(posting = false, error = e.message) } },
            )
        }
    }

    fun toggleLike(postId: String) {
        val uid = auth.currentUserId() ?: return
        val nowLiked = postId !in _state.value.liked
        _state.update { it.copy(liked = if (nowLiked) it.liked + postId else it.liked - postId) }
        viewModelScope.launch {
            repo.setLiked(uid, postId, nowLiked).onFailure { e ->
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
