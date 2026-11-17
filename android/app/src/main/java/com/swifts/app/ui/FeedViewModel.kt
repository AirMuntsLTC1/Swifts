package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = false,
    val items: List<Post> = emptyList(),
    val error: String? = null,
)

class FeedViewModel(
    private val repo: PostRepository,
    private val lang: String,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repo.feed(lang).fold(
                onSuccess = { page -> _state.value = FeedUiState(items = page.items) },
                onFailure = { e -> _state.value = FeedUiState(error = e.message) },
            )
        }
    }
}
