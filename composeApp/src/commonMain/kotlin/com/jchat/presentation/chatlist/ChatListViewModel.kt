package com.jchat.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.model.Chat
import com.jchat.domain.model.Profile
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── MVI Contract ─────────────────────────────────────────────────────────────

/** All UI state for the chat-list screen. */
data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val isLoading: Boolean = true,
    val isCreatingChat: Boolean = false,
    val errorMessage: String? = null,
    // User search state for the new-chat bottom sheet.
    val userSearchQuery: String = "",
    val userSearchResults: List<Profile> = emptyList(),
    val isSearchingUser: Boolean = false,
)

/** User-triggered intents for the chat-list screen. */
sealed interface ChatListIntent {
    data class OpenChat(val chatId: String) : ChatListIntent
    data object Refresh : ChatListIntent
    data object DismissError : ChatListIntent
    data class UpdateSearchQuery(val query: String) : ChatListIntent
    data class ToggleSearchMode(val enabled: Boolean) : ChatListIntent
    data class UpdateUserSearchQuery(val query: String) : ChatListIntent
    data class SelectUser(val profile: Profile) : ChatListIntent
    data object ClearUserSearch : ChatListIntent
}

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

    private var observeJob: kotlinx.coroutines.Job? = null
    private val userSearchQueryFlow = MutableStateFlow("")

    init {
        observeChats()
        observeUserSearch()
    }

    fun onIntent(intent: ChatListIntent) {
        when (intent) {
            is ChatListIntent.OpenChat -> navigateToConversation(intent.chatId)
            ChatListIntent.Refresh -> observeChats()
            ChatListIntent.DismissError -> _state.update { it.copy(errorMessage = null) }
            is ChatListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is ChatListIntent.ToggleSearchMode -> _state.update {
                it.copy(isSearchMode = intent.enabled, searchQuery = if (intent.enabled) it.searchQuery else "")
            }
            is ChatListIntent.UpdateUserSearchQuery -> {
                _state.update { it.copy(userSearchQuery = intent.query) }
                userSearchQueryFlow.value = intent.query
            }
            is ChatListIntent.SelectUser -> selectUser(intent.profile)
            ChatListIntent.ClearUserSearch -> _state.update {
                it.copy(userSearchQuery = "", userSearchResults = emptyList(), isSearchingUser = false)
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { s ->
            val filtered = if (query.isBlank()) s.chats else {
                s.chats.filter {
                    it.participant.displayName.contains(query, ignoreCase = true) ||
                        it.participant.username.contains(query, ignoreCase = true)
                }
            }
            s.copy(searchQuery = query, filteredChats = filtered)
        }
    }

    private fun observeUserSearch() {
        viewModelScope.launch {
            userSearchQueryFlow
                .debounce(300L)
                .collectLatest { query ->
                    val normalized = query.trim().removePrefix("@").trim()
                    if (normalized.length < 3) {
                        _state.update { it.copy(userSearchResults = emptyList(), isSearchingUser = false) }
                        return@collectLatest
                    }

                    _state.update { it.copy(isSearchingUser = true, errorMessage = null) }
                    val results = runCatching {
                        repository.searchUsers(normalized)
                    }.getOrElse { error ->
                        _state.update { it.copy(errorMessage = error.message, isSearchingUser = false) }
                        return@collectLatest
                    }

                    _state.update {
                        it.copy(
                            userSearchResults = results,
                            isSearchingUser = false,
                        )
                    }
                }
        }
    }

    private fun selectUser(profile: Profile) {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingChat = true, errorMessage = null) }
            try {
                val chatId = repository.startChatWithUser(profile)
                _events.emit(ChatListEvent.NavigateToConversation(chatId))
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message, isCreatingChat = false) }
            } finally {
                _state.update { it.copy(isCreatingChat = false) }
            }
        }
    }

    private fun observeChats() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.observeChats()
                .catch { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { chats ->
                    _state.update {
                        it.copy(
                            chats = chats,
                            filteredChats = if (it.searchQuery.isBlank()) chats else chats.filter { c ->
                                c.participant.displayName.contains(it.searchQuery, ignoreCase = true) ||
                                c.participant.username.contains(it.searchQuery, ignoreCase = true)
                            },
                            isLoading = false
                        )
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
