package com.swifts.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swifts.app.domain.AuthRepository
import com.swifts.app.domain.Group
import com.swifts.app.domain.GroupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val query: String = "",
    val loading: Boolean = false,
    val groups: List<Group> = emptyList(),
    val joined: Set<String> = emptySet(),
    val showCreate: Boolean = false,
    val creating: Boolean = false,
    val error: String? = null,
)

class GroupsViewModel(
    private val groups: GroupRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        search("")
        auth.currentUserId()?.let { uid ->
            viewModelScope.launch {
                groups.joinedGroupIds(uid).onSuccess { ids -> _state.update { it.copy(joined = ids) } }
            }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search(q)
        }
    }

    private fun search(q: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            groups.listGroups(q).fold(
                onSuccess = { list -> _state.update { it.copy(loading = false, groups = list) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    fun setShowCreate(on: Boolean) {
        _state.update { it.copy(showCreate = on, error = null) }
    }

    fun create(name: String, description: String, privacy: String) {
        val uid = auth.currentUserId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(creating = true, error = null) }
            groups.create(uid, name.trim(), description.trim(), privacy).fold(
                onSuccess = { g ->
                    _state.update {
                        it.copy(
                            creating = false,
                            showCreate = false,
                            groups = listOf(g) + it.groups,
                            joined = it.joined + g.id,
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(creating = false, error = e.message) } },
            )
        }
    }

    fun toggleJoin(group: Group) {
        val uid = auth.currentUserId() ?: return
        val nowJoined = group.id !in _state.value.joined
        _state.update {
            it.copy(joined = if (nowJoined) it.joined + group.id else it.joined - group.id)
        }
        viewModelScope.launch {
            groups.setJoined(group.id, uid, nowJoined).onFailure {
                _state.update {
                    it.copy(joined = if (nowJoined) it.joined - group.id else it.joined + group.id)
                }
            }
        }
    }
}
