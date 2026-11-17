package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Group
import com.swifts.app.domain.GroupRepository
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val loading: Boolean = true,
    val group: Group? = null,
    val joined: Boolean = false,
    val posts: List<Post> = emptyList(),
    val posting: Boolean = false,
    val error: String? = null,
)

class GroupDetailViewModel(
    private val groups: GroupRepository,
    private val posts: PostRepository,
    private val auth: AuthRepository,
    private val groupId: String,
    private val lang: String,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupDetailUiState())
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val group = groups.group(groupId).getOrElse { e ->
                _state.update { it.copy(loading = false, error = e.message) }
                return@launch
            }
            val feed = posts.postsByGroup(groupId).getOrElse { emptyList() }
            val joined = auth.currentUserId()?.let { uid ->
                groups.joinedGroupIds(uid).getOrElse { emptySet() }.contains(groupId)
            } ?: false
            _state.update { it.copy(loading = false, group = group, posts = feed, joined = joined) }
        }
    }

    fun toggleMembership() {
        val uid = auth.currentUserId() ?: return
        val nowJoined = !_state.value.joined
        _state.update { it.copy(joined = nowJoined) }
        viewModelScope.launch {
            groups.setJoined(groupId, uid, nowJoined).onFailure { e ->
                _state.update { it.copy(joined = !nowJoined, error = e.message) }
            }
        }
    }

    fun publish(text: String) {
        val uid = auth.currentUserId() ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(posting = true, error = null) }
            posts.createPost(uid, body, lang, groupId = groupId).fold(
                onSuccess = { post -> _state.update { it.copy(posting = false, posts = listOf(post) + it.posts) } },
                onFailure = { e -> _state.update { it.copy(posting = false, error = e.message) } },
            )
        }
    }
}
