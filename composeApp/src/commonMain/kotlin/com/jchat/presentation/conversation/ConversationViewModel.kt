package com.jchat.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.model.ContentType
import com.jchat.domain.model.Message
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── MVI Contract ─────────────────────────────────────────────────────────────

/** All UI state for a single conversation screen. */
data class ConversationState(
    val messages: List<Message> = emptyList(),
    val participantName: String = "",
    val participantAvatarUrl: String? = null,
    val participantStatus: String = "Offline",
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isTyping: Boolean = false,
    val uploadProgress: Float? = null,
    val errorMessage: String? = null,
)

/** User-triggered intents for the conversation screen. */
sealed interface ConversationIntent {
    /** User typed text in the message field. */
    data class UpdateInput(val text: String) : ConversationIntent

    /** User tapped "Send" with a text message. */
    data object SendTextMessage : ConversationIntent

    /** User selected a media file to send. */
    data class SendMediaMessage(
        val localPath: String,
        val contentType: ContentType,
    ) : ConversationIntent

    /** User long-pressed a message and chose "Delete". */
    data class DeleteMessage(val messageId: String) : ConversationIntent

    /** User dismisses the error snackbar. */
    data object DismissError : ConversationIntent
}

/** One-shot side effects emitted by the ViewModel. */
sealed interface ConversationEvent {
    /** Scrolls the list to the last message. */
    data object ScrollToBottom : ConversationEvent
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ConversationViewModel(
    private val chatId: String,
    private val repository: IChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state

    private val _events = MutableSharedFlow<ConversationEvent>()
    val events: SharedFlow<ConversationEvent> = _events.asSharedFlow()

    init {
        loadParticipant()
        observeMessages()
        viewModelScope.launch {
            repository.markChatAsRead(chatId)
            repository.subscribeToRealtimeMessages(chatId)
        }
        viewModelScope.launch {
            repository.syncMessages(chatId)
        }
    }

    fun onIntent(intent: ConversationIntent) {
        when (intent) {
            is ConversationIntent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            ConversationIntent.SendTextMessage -> sendText()
            is ConversationIntent.SendMediaMessage -> sendMedia(intent.localPath, intent.contentType)
            is ConversationIntent.DeleteMessage -> deleteMessage(intent.messageId)
            ConversationIntent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private fun loadParticipant() {
        viewModelScope.launch {
            repository.observeChats().collect { chatList ->
                val chat = chatList.find { it.id == chatId }
                chat?.let {
                    _state.update { s ->
                        s.copy(
                            participantName = it.participant.displayName.ifBlank { it.participant.username },
                            participantAvatarUrl = it.participant.avatarUrl,
                            participantStatus = when (it.participant.status) {
                                com.jchat.domain.model.OnlineStatus.ONLINE -> "Online"
                                com.jchat.domain.model.OnlineStatus.AWAY -> "Away"
                                com.jchat.domain.model.OnlineStatus.OFFLINE -> "Offline"
                            },
                        )
                    }
                }
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.observeMessages(chatId)
                .catch { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { messages ->
                    _state.update { it.copy(messages = messages, isLoading = false) }
                    _events.emit(ConversationEvent.ScrollToBottom)
                }
        }
    }

    private fun sendText() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        _state.update { it.copy(inputText = "", isSending = true) }

        viewModelScope.launch {
            runCatching { repository.sendTextMessage(chatId, text) }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message, isSending = false) }
                }
                .onSuccess {
                    _state.update { it.copy(isSending = false) }
                }
        }
    }

    private fun sendMedia(localPath: String, contentType: ContentType) {
        viewModelScope.launch {
            repository.sendMediaMessage(chatId, localPath, contentType)
                .catch { e -> _state.update { it.copy(errorMessage = e.message, uploadProgress = null) } }
                .collect { progress ->
                    _state.update { it.copy(uploadProgress = if (progress < 1f) progress else null) }
                }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMessage(messageId) }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.unsubscribeFromRealtimeMessages(chatId)
        }
    }
}
