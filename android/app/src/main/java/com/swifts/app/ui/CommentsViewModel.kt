package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Comment
import com.swifts.app.domain.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Who the next comment replies to. rootId is the top-level comment, so replies stay one level deep. */
data class ReplyTarget(val rootId: String, val username: String)

data class CommentsUiState(
    val loading: Boolean = true,
    val comments: List<Comment> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val liked: Set<String> = emptySet(),
    val replyTo: ReplyTarget? = null,
    val posting: Boolean = false,
    val error: String? = null,
)

class CommentsViewModel(
    private val repo: CommentRepository,
    private val auth: AuthRepository,
    private val postId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(CommentsUiState())
    val state: StateFlow<CommentsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val list = repo.list(postId).getOrElse { e ->
                _state.update { it.copy(loading = false, error = e.message) }
                return@launch
            }
            val likes = repo.likes(list.map { it.id }).getOrElse { emptyList() }
            val uid = auth.currentUserId()
            _state.update { s ->
                s.copy(
                    loading = false,
                    comments = list,
                    counts = likes.groupingBy { it.comment_id }.eachCount(),
                    liked = likes.filter { it.user_id == uid }.map { it.comment_id }.toSet(),
                )
            }
        }
    }

    fun setReply(rootId: String?, username: String) {
        _state.update { s -> s.copy(replyTo = rootId?.let { ReplyTarget(it, username) }) }
    }

    fun send(text: String) {
        val uid = auth.currentUserId() ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        val parent = _state.value.replyTo?.rootId
        viewModelScope.launch {
            _state.update { it.copy(posting = true, error = null) }
            repo.add(postId, uid, body, parent).fold(
                onSuccess = { c ->
                    _state.update { s -> s.copy(posting = false, replyTo = null, comments = s.comments + c) }
                },
                onFailure = { e -> _state.update { it.copy(posting = false, error = e.message) } },
            )
        }
    }

    fun toggleLike(commentId: String) {
        val uid = auth.currentUserId() ?: return
        val nowLiked = commentId !in _state.value.liked
        applyLike(commentId, nowLiked)
        viewModelScope.launch {
            repo.setLiked(commentId, uid, nowLiked).onFailure { applyLike(commentId, !nowLiked) }
        }
    }

    fun delete(commentId: String) {
        viewModelScope.launch {
            repo.delete(commentId).onSuccess {
                _state.update { s ->
                    val gone = descendants(s.comments, commentId)
                    s.copy(comments = s.comments.filter { it.id !in gone })
                }
            }
        }
    }

    private fun applyLike(commentId: String, liked: Boolean) {
        _state.update { s ->
            val delta = if (liked) 1 else -1
            val count = ((s.counts[commentId] ?: 0) + delta).coerceAtLeast(0)
            s.copy(
                liked = if (liked) s.liked + commentId else s.liked - commentId,
                counts = s.counts + (commentId to count),
            )
        }
    }

    /** The comment and every reply under it. */
    private fun descendants(all: List<Comment>, root: String): Set<String> {
        val gone = mutableSetOf(root)
        var changed = true
        while (changed) {
            changed = false
            all.forEach { c ->
                val parent = c.parent_id
                if (parent != null && parent in gone && gone.add(c.id)) changed = true
            }
        }
        return gone
    }
}
