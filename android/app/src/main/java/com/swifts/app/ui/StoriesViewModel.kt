package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.MediaStorage
import com.swifts.app.domain.ProfileRepository
import com.swifts.app.domain.Story
import com.swifts.app.domain.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** One bubble per person: all their active stories, newest first. */
data class StoryBubble(
    val authorId: String,
    val username: String,
    val avatarUrl: String?,
    val stories: List<Story>,
)

data class StoriesUiState(
    val bubbles: List<StoryBubble> = emptyList(),
    val uploading: Boolean = false,
    val viewing: StoryBubble? = null,
    val error: String? = null,
)

class StoriesViewModel(
    private val stories: StoryRepository,
    private val profiles: ProfileRepository,
    private val storage: MediaStorage,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StoriesUiState())
    val state: StateFlow<StoriesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val uid = auth.currentUserId() ?: return
        viewModelScope.launch {
            val following = profiles.followingIds(uid).getOrElse { emptySet() }
            val list = stories.visibleStories((following + uid).toList()).getOrElse { e ->
                _state.update { it.copy(error = e.message) }
                return@launch
            }
            val bubbles = list.groupBy { it.author_id }.map { (authorId, items) ->
                val author = items.first().author
                StoryBubble(authorId, author?.username ?: "user", author?.avatar_url, items)
            }
            _state.update { it.copy(bubbles = bubbles, error = null) }
        }
    }

    fun upload(bytes: ByteArray, mime: String) {
        val uid = auth.currentUserId() ?: return
        if (bytes.size > MAX_BYTES) {
            _state.update { it.copy(error = "Image is larger than 8 MB") }
            return
        }
        val ext = if (mime.contains("png")) "png" else "jpg"
        val path = "$uid/${UUID.randomUUID()}.$ext"
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, error = null) }
            storage.upload("stories", path, bytes, mime).fold(
                onSuccess = {
                    stories.create(uid, path).fold(
                        onSuccess = {
                            _state.update { it.copy(uploading = false) }
                            load()
                        },
                        onFailure = { e -> _state.update { it.copy(uploading = false, error = e.message) } },
                    )
                },
                onFailure = { e -> _state.update { it.copy(uploading = false, error = e.message) } },
            )
        }
    }

    fun publicUrl(path: String): String = storage.publicUrl("stories", path)

    fun open(bubble: StoryBubble) { _state.update { it.copy(viewing = bubble) } }

    fun close() { _state.update { it.copy(viewing = null) } }

    private companion object {
        const val MAX_BYTES = 8 * 1024 * 1024
    }
}
