package com.jchat.data.repository

import com.benasher44.uuid.uuid4
import com.jchat.data.local.LocalDataSource
import com.jchat.data.remote.MessageDto
import com.jchat.data.remote.RemoteDataSource
import com.jchat.data.remote.toDomain
import com.jchat.domain.model.Chat
import com.jchat.domain.model.ContentType
import com.jchat.domain.model.Message
import com.jchat.domain.model.MessageStatus
import com.jchat.domain.model.Profile
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Offline-first implementation of [IChatRepository].
 *
 * ## Read Strategy
 * All [Flow]-based reads are backed by SQLDelight reactive queries.
 * Any change in the local DB (whether from user action or background sync)
 * is automatically propagated to the UI.
 *
 * ## Write Strategy
 * 1. **Optimistic local write** – the message is immediately inserted into
 *    SQLDelight with status [MessageStatus.SENDING], making the UI responsive.
 * 2. **Remote write** – the message is sent to Supabase in the background.
 * 3. **Status update** – the local record is updated to [MessageStatus.SENT]
 *    on success or [MessageStatus.FAILED] on error.
 */
class ChatRepositoryImpl(
    private val local: LocalDataSource,
    private val remote: RemoteDataSource,
) : IChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─── Profile ──────────────────────────────────────────────────────────────

    override suspend fun getCurrentProfile(): Profile? = withContext(Dispatchers.IO) {
        val userId = remote.getCurrentUserId() ?: return@withContext null
        getProfile(userId)
    }

    override suspend fun getProfile(userId: String): Profile? = withContext(Dispatchers.IO) {
        local.getProfileById(userId)
            ?: remote.fetchProfile(userId)?.toDomain()?.also { local.upsertProfile(it) }
    }

    override suspend fun updateProfile(displayName: String, avatarUrl: String?) = withContext(Dispatchers.IO) {
        val currentProfile = getCurrentProfile() ?: return@withContext
        val updatedProfile = currentProfile.copy(
            displayName = displayName,
            avatarUrl = avatarUrl ?: currentProfile.avatarUrl,
        )
        local.upsertProfile(updatedProfile)
        // In a real app, we would also push this to Supabase.
    }

    // ─── Chats ────────────────────────────────────────────────────────────────

    override fun observeChats(): Flow<List<Chat>> = local.observeChats()

    override suspend fun markChatAsRead(chatId: String) = withContext(Dispatchers.IO) {
        local.markChatAsRead(chatId)
    }

    // ─── Messages ─────────────────────────────────────────────────────────────

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        local.observeMessages(chatId)

    /**
     * Sends a text message using an optimistic local-write strategy:
     *
     * 1. Create a [Message] with status [MessageStatus.SENDING] and insert it locally.
     * 2. Launch a background coroutine to push to Supabase.
     * 3. Update the local status to [MessageStatus.SENT] or [MessageStatus.FAILED].
     */
    override suspend fun sendTextMessage(chatId: String, content: String) {
        val currentUserId = remote.getCurrentUserId()
            ?: error("User is not authenticated")

        val now = Clock.System.now()
        val message = Message(
            id = uuid4().toString(),
            chatId = chatId,
            senderId = currentUserId,
            content = content,
            contentType = ContentType.TEXT,
            status = MessageStatus.SENDING,
            createdAt = now,
            updatedAt = now,
        )

        // 1. Optimistic local insert
        withContext(Dispatchers.IO) { local.insertMessage(message) }

        // 2. Update chat preview locally
        withContext(Dispatchers.IO) {
            local.updateChatLastMessage(chatId, content, now.toEpochMilliseconds())
        }

        // 3. Remote write
        scope.launch {
            try {
                val dto = MessageDto(
                    id = message.id,
                    chatId = message.chatId,
                    senderId = message.senderId,
                    content = message.content,
                    contentType = message.contentType.name,
                    mediaUrl = message.mediaUrl,
                    status = "SENT",
                    createdAt = message.createdAt.toString(),
                    updatedAt = message.updatedAt.toString()
                )
                remote.sendMessage(dto)
                
                withContext(Dispatchers.IO) {
                    local.updateMessageStatus(
                        messageId = message.id,
                        status = MessageStatus.SENT,
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    local.updateMessageStatus(
                        messageId = message.id,
                        status = MessageStatus.FAILED,
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            }
        }
    }

    /**
     * Internal listener job for real-time messages.
     * In a production app, manage this carefully to avoid multiple observers per chat.
     */
    private val realtimeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    override suspend fun syncMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                val remoteMessages = remote.fetchMessages(chatId)
                remoteMessages.forEach { dto ->
                    local.upsertMessage(dto.toDomain())
                }
            } catch (e: Exception) {
                // Silently fail or log – UI will show whatever is in local DB.
            }
        }
    }

    override suspend fun subscribeToRealtimeMessages(chatId: String) {
        if (realtimeJobs.containsKey(chatId)) return

        val job = scope.launch {
            remote.subscribeToMessages(chatId).collect { dto ->
                // Skip if it's our own message (already handled optimistically)
                if (dto.senderId != remote.getCurrentUserId()) {
                    withContext(Dispatchers.IO) {
                        val message = dto.toDomain()
                        local.upsertMessage(message)
                        local.updateChatLastMessage(
                            chatId = chatId,
                            preview = message.content ?: "Media message",
                            timestamp = message.createdAt.toEpochMilliseconds()
                        )
                    }
                }
            }
        }
        realtimeJobs[chatId] = job
    }
override suspend fun unsubscribeFromRealtimeMessages(chatId: String) {
    realtimeJobs[chatId]?.cancel()
    realtimeJobs.remove(chatId)
    remote.unsubscribeFromMessages(chatId)
}

/**

    override fun sendMediaMessage(
        chatId: String,
        mediaLocalPath: String,
        contentType: ContentType,
    ): Flow<Float> = flow {
        val currentUserId = remote.getCurrentUserId()
            ?: error("User is not authenticated")

        val now = Clock.System.now()
        val messageId = uuid4().toString()
        val ext = mediaLocalPath.substringAfterLast('.', "bin")
        val remotePath = "chat-media/$chatId/$messageId.$ext"

        // 1. Insert placeholder message locally
        val message = Message(
            id = messageId,
            chatId = chatId,
            senderId = currentUserId,
            content = null,
            contentType = contentType,
            mediaLocalPath = mediaLocalPath,
            status = MessageStatus.SENDING,
            createdAt = now,
            updatedAt = now,
        )
        withContext(Dispatchers.IO) { local.insertMessage(message) }

        // 2. Real upload via Supabase Storage
        var publicUrl: String? = null
        try {
            val bytes = readFileBytes(mediaLocalPath)
            remote.uploadMedia("chat-media", remotePath, bytes).collect { url ->
                publicUrl = url
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                local.updateMessageStatus(messageId, MessageStatus.FAILED, Clock.System.now().toEpochMilliseconds())
            }
            return@flow
        }

        // 3. Send message metadata to Database
        if (publicUrl != null) {
            try {
                val dto = MessageDto(
                    id = messageId,
                    chatId = chatId,
                    senderId = currentUserId,
                    content = null,
                    contentType = contentType.name,
                    mediaUrl = publicUrl,
                    status = "SENT",
                    createdAt = now.toString(),
                    updatedAt = now.toString()
                )
                remote.sendMessage(dto)

                withContext(Dispatchers.IO) {
                    local.updateMessageMediaUrl(
                        messageId = messageId,
                        mediaUrl = publicUrl!!,
                        status = MessageStatus.SENT,
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    local.updateMessageStatus(messageId, MessageStatus.FAILED, Clock.System.now().toEpochMilliseconds())
                }
            }
        }
        emit(1.0f)
    }

    override suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        local.softDeleteMessage(messageId, Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Reads the bytes from a local file path.
     * Expect-actual per platform because file I/O APIs differ between Android and iOS.
     */
    private fun readFileBytes(path: String): ByteArray {
        // This is a simplified implementation. In production, use an expect/actual
        // platform function, or use the Okio library for cross-platform file I/O.
        throw NotImplementedError("readFileBytes must be implemented per platform")
    }
}
