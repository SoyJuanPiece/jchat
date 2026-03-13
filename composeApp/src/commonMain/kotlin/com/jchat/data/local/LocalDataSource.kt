package com.jchat.data.local

import com.jchat.db.JChatDatabase
import com.jchat.domain.model.Chat
import com.jchat.domain.model.ContentType
import com.jchat.domain.model.Message
import com.jchat.domain.model.MessageStatus
import com.jchat.domain.model.OnlineStatus
import com.jchat.domain.model.Profile
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.jchat.db.Messages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * Data Access Object wrapping all SQLDelight queries.
 *
 * All queries are executed on [Dispatchers.IO] to avoid blocking the main thread.
 */
class LocalDataSource(private val db: JChatDatabase) {

    private val messagesQueries = db.messagesQueries
    private val profilesQueries = db.profilesQueries
    private val settingsQueries = db.settingsQueries

    // ─── Profiles ─────────────────────────────────────────────────────────────

    fun upsertProfile(profile: Profile) {
        profilesQueries.upsertProfile(
            id = profile.id,
            username = profile.username,
            display_name = profile.displayName,
            avatar_url = profile.avatarUrl,
            status = profile.status.name.lowercase(),
            last_seen_at = profile.lastSeenAt?.toEpochMilliseconds() ?: 0L,
            created_at = profile.createdAt.toEpochMilliseconds(),
        )
    }

    fun getProfileById(id: String): Profile? =
        profilesQueries.getProfileById(id).executeAsOneOrNull()?.toProfile()

    // ─── Chats ────────────────────────────────────────────────────────────────

    fun observeChats(): Flow<List<Chat>> =
        messagesQueries.getAllChats()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toChat() } }

    fun upsertChat(chat: Chat) {
        messagesQueries.upsertChat(
            id = chat.id,
            participant_id = chat.participant.id,
            last_message_preview = chat.lastMessagePreview,
            last_message_at = chat.lastMessageAt?.toEpochMilliseconds() ?: 0L,
            unread_count = chat.unreadCount.toLong(),
            created_at = chat.createdAt.toEpochMilliseconds(),
        )
    }

    fun updateChatLastMessage(
        chatId: String,
        preview: String,
        timestamp: Long,
        incrementUnread: Boolean,
    ) {
        if (incrementUnread) {
            messagesQueries.updateChatLastMessage(
                last_message_preview = preview,
                last_message_at = timestamp,
                id = chatId,
            )
        } else {
            messagesQueries.updateChatLastMessageWithoutUnread(
                last_message_preview = preview,
                last_message_at = timestamp,
                id = chatId,
            )
        }
    }

    fun markChatAsRead(chatId: String) {
        messagesQueries.markChatAsRead(chatId)
    }

    fun getChatIdByParticipantId(participantId: String): String? =
        messagesQueries.getChatIdByParticipantId(participantId).executeAsOneOrNull()

    // ─── Messages ─────────────────────────────────────────────────────────────

    fun observeMessages(chatId: String): Flow<List<Message>> =
        messagesQueries.getMessagesByChatId(chatId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toMessage() } }

    fun insertMessage(message: Message) {
        messagesQueries.insertMessage(
            id = message.id,
            chat_id = message.chatId,
            sender_id = message.senderId,
            content = message.content,
            content_type = message.contentType.name.lowercase(),
            media_url = message.mediaUrl,
            media_local_path = message.mediaLocalPath,
            reply_to_message_id = message.replyToMessageId,
            reply_preview = message.replyPreview,
            status = message.status.name.lowercase(),
            created_at = message.createdAt.toEpochMilliseconds(),
            updated_at = message.updatedAt.toEpochMilliseconds(),
        )
    }

    fun updateMessageStatus(messageId: String, status: MessageStatus, updatedAt: Long) {
        messagesQueries.updateMessageStatus(
            status = status.name.lowercase(),
            updated_at = updatedAt,
            id = messageId,
        )
    }

    fun updateMessageMediaUrl(messageId: String, mediaUrl: String, status: MessageStatus, updatedAt: Long) {
        messagesQueries.updateMessageMediaUrl(
            media_url = mediaUrl,
            status = status.name.lowercase(),
            updated_at = updatedAt,
            id = messageId,
        )
    }

    fun softDeleteMessage(messageId: String, updatedAt: Long) {
        messagesQueries.softDeleteMessage(updated_at = updatedAt, id = messageId)
    }

    fun upsertMessage(message: Message) {
        messagesQueries.upsertMessage(
            id = message.id,
            chat_id = message.chatId,
            sender_id = message.senderId,
            content = message.content,
            content_type = message.contentType.name.lowercase(),
            media_url = message.mediaUrl,
            media_local_path = message.mediaLocalPath,
            reply_to_message_id = message.replyToMessageId,
            reply_preview = message.replyPreview,
            status = message.status.name.lowercase(),
            created_at = message.createdAt.toEpochMilliseconds(),
            updated_at = message.updatedAt.toEpochMilliseconds(),
        )
    }

    fun getPendingMessages(): List<Message> =
        messagesQueries.getPendingMessages().executeAsList().map { it.toMessage() }

    fun getMessageById(messageId: String): Message? =
        messagesQueries.getMessageById(messageId).executeAsOneOrNull()?.toMessage()

    fun clearAllData() {
        // Respect FK dependencies: messages -> chats -> profiles.
        messagesQueries.clearMessages()
        messagesQueries.clearChats()
        profilesQueries.clearProfiles()
    }

    // ─── App Settings ────────────────────────────────────────────────────────

    fun setSetting(key: String, value: String, updatedAt: Long) {
        settingsQueries.upsertSetting(
            key = key,
            value_ = value,
            updated_at = updatedAt,
        )
    }

    fun getSetting(key: String): String? =
        settingsQueries.getSettingByKey(key).executeAsOneOrNull()
}

// ─── Extension Mappers ───────────────────────────────────────────────────────

private fun com.jchat.db.Profiles.toProfile(): Profile = Profile(
    id = id,
    username = username,
    displayName = display_name,
    avatarUrl = avatar_url,
    status = OnlineStatus.fromString(status),
    lastSeenAt = if (last_seen_at > 0) Instant.fromEpochMilliseconds(last_seen_at) else null,
    createdAt = Instant.fromEpochMilliseconds(created_at),
)

private fun com.jchat.db.GetAllChats.toChat(): Chat = Chat(
    id = id,
    participant = Profile(
        id = participant_id,
        username = participant_username,
        displayName = participant_name,
        avatarUrl = participant_avatar,
        status = OnlineStatus.fromString(participant_status),
        createdAt = Instant.fromEpochMilliseconds(0),
    ),
    lastMessagePreview = last_message_preview,
    lastMessageAt = if (last_message_at > 0) Instant.fromEpochMilliseconds(last_message_at) else null,
    unreadCount = unread_count.toInt(),
    createdAt = Instant.fromEpochMilliseconds(created_at),
)

private fun Messages.toMessage(): Message = Message(
    id = id,
    chatId = chat_id,
    senderId = sender_id,
    content = content,
    contentType = ContentType.fromString(content_type),
    mediaUrl = media_url,
    mediaLocalPath = media_local_path,
    replyToMessageId = reply_to_message_id,
    replyPreview = reply_preview,
    status = MessageStatus.fromString(status),
    createdAt = Instant.fromEpochMilliseconds(created_at),
    updatedAt = Instant.fromEpochMilliseconds(updated_at),
    isDeleted = is_deleted != 0L,
)
