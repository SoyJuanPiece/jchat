package com.jchat.domain.repository

import com.jchat.domain.model.Chat
import com.jchat.domain.model.Message
import com.jchat.domain.model.Profile
import kotlinx.coroutines.flow.Flow

/**
 * Contract for all chat-related data operations.
 *
 * Implementations should follow an **offline-first** strategy:
 * - Reads come from the local SQLDelight database.
 * - Network sync happens in the background and updates the local DB,
 *   which in turn updates the exposed [Flow]s automatically.
 */
interface IChatRepository {

    // ─── Profile ──────────────────────────────────────────────────────────────

    /** Returns the currently signed-in user profile, or null if not authenticated. */
    suspend fun getCurrentProfile(): Profile?

    /** Fetches a profile by [userId] (local cache first, then remote). */
    suspend fun getProfile(userId: String): Profile?

    /** Updates the current user's profile locally and remotely. */
    suspend fun updateProfile(displayName: String, avatarUrl: String?)

    /** Signs out the current user and clears local session data. */
    suspend fun signOut()

    /** Starts a new chat with the user matching [username]. Returns the new chatId. */
    suspend fun startChat(username: String): String

    // ─── Chats ────────────────────────────────────────────────────────────────

    /**
     * Streams the full list of conversations, ordered by the most recent message.
     * Emits a new list whenever a local DB change is detected.
     */
    fun observeChats(): Flow<List<Chat>>

    /** Marks all messages in [chatId] as read and resets the unread counter. */
    suspend fun markChatAsRead(chatId: String)

    // ─── Messages ─────────────────────────────────────────────────────────────

    /**
     * Streams messages for a given [chatId], ordered chronologically (oldest first).
     * Emits whenever a new message arrives or an existing one is updated.
     */
    fun observeMessages(chatId: String): Flow<List<Message>>

    /**
     * Sends a text message in [chatId].
     *
     * Steps performed:
     * 1. Persist locally with status [com.jchat.domain.model.MessageStatus.SENDING].
     * 2. Attempt to send via Supabase / Ktor.
     * 3. Update local status to [com.jchat.domain.model.MessageStatus.SENT] on success,
     *    or [com.jchat.domain.model.MessageStatus.FAILED] on error.
     */
    suspend fun sendTextMessage(chatId: String, content: String)

    /**
     * Uploads [mediaLocalPath] to Supabase Storage and then sends a message with the
     * resulting remote URL. Upload progress is reported via the returned [Flow<Float>].
     *
     * @param chatId Target conversation.
     * @param mediaLocalPath Absolute path to the local file.
     * @param contentType Type of media (IMAGE, AUDIO, VIDEO, FILE).
     * @return A [Flow] that emits upload progress in the range [0.0, 1.0].
     */
    fun sendMediaMessage(
        chatId: String,
        mediaLocalPath: String,
        contentType: com.jchat.domain.model.ContentType,
    ): Flow<Float>

    /**
     * Soft-deletes a message locally and requests deletion from the remote backend.
     */
    suspend fun deleteMessage(messageId: String)

    // ─── Sync ─────────────────────────────────────────────────────────────────

    /**
     * Performs a one-shot background sync: fetches latest remote messages for
     * [chatId] and persists any new/updated ones in the local DB.
     */
    suspend fun syncMessages(chatId: String)

    /**
     * Subscribes to Supabase Realtime events for [chatId]. Any incoming event
     * is automatically persisted to the local DB.
     */
    suspend fun subscribeToRealtimeMessages(chatId: String)

    /** Cancels the active Realtime subscription for [chatId]. */
    suspend fun unsubscribeFromRealtimeMessages(chatId: String)
}
