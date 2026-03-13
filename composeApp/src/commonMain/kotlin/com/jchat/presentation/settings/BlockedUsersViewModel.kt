package com.jchat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.model.Profile
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersState(
    val users: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface BlockedUsersIntent {
    data class UnblockUser(val userId: String) : BlockedUsersIntent
    data object Refresh : BlockedUsersIntent
    data object DismissError : BlockedUsersIntent
}

class BlockedUsersViewModel(
    private val repository: IChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedUsersState())
    val state: StateFlow<BlockedUsersState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onIntent(intent: BlockedUsersIntent) {
        when (intent) {
            is BlockedUsersIntent.UnblockUser -> unblock(intent.userId)
            BlockedUsersIntent.Refresh -> refresh()
            BlockedUsersIntent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                repository.getBlockedUsers()
            }.onSuccess { users ->
                _state.update { it.copy(users = users, isLoading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar los bloqueados"
                    )
                }
            }
        }
    }

    private fun unblock(userId: String) {
        _state.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            runCatching {
                repository.unblockUser(userId)
                repository.getBlockedUsers()
            }.onSuccess { users ->
                _state.update { it.copy(users = users, isUpdating = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = error.message ?: "No se pudo desbloquear el usuario"
                    )
                }
            }
        }
    }
}
