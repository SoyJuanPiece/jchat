package com.jchat.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.model.Chat
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── MVI Contract ─────────────────────────────────────────────────────────────

/** All UI state for the chat-list screen. */
data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** User-triggered intents for the chat-list screen. */
sealed interface ChatListIntent {
    /** User opened a conversation. */
    data class OpenChat(val chatId: String) : ChatListIntent

    /** User pulls down to refresh. */
    data object Refresh : ChatListIntent

    /** User dismisses the error snackbar. */
    data object DismissError : ChatListIntent
}

/** One-shot navigation events emitted by the ViewModel. */
sealed interface ChatListEvent {
    data class NavigateToConversation(val chatId: String) : ChatListEvent
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ChatListViewModel(
    private val repository: IChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state

    private val _events = MutableSharedFlow<ChatListEvent>()
    val events: SharedFlow<ChatListEvent> = _events.asSharedFlow()

    /** Single, cancellable job that collects the chats flow. */
    private var observeJob: kotlinx.coroutines.Job? = null

    init {
        observeChats()
    }

    fun onIntent(intent: ChatListIntent) {
        when (intent) {
            is ChatListIntent.OpenChat -> navigateToConversation(intent.chatId)
            ChatListIntent.Refresh -> observeChats()
            ChatListIntent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun observeChats() {
        // Cancel any existing collector before starting a new one to avoid duplicates.
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.observeChats()
                .catch { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { chats ->
                    _state.update {
                        it.copy(chats = chats, isLoading = false)
                    }
                }
        }
    }

    private fun navigateToConversation(chatId: String) {
        viewModelScope.launch {
            _events.emit(ChatListEvent.NavigateToConversation(chatId))
        }
    }
}
