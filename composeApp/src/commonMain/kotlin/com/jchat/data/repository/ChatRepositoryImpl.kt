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

    override suspend fun updateProfile(displayName: String, avatar_url: String?) = withContext(Dispatchers.IO) {
        val userId = remote.getCurrentUserId() ?: return@withContext
        
        // 1. Update Supabase
        try {
            remote.updateProfile(userId, displayName, avatar_url)
        } catch (e: Exception) {
            // Log error or rethrow
        }

        // 2. Update local
        val currentProfile = local.getProfileById(userId)
        if (currentProfile != null) {
            val updatedProfile = currentProfile.copy(
                displayName = displayName,
                avatarUrl = avatar_url ?: currentProfile.avatarUrl,
            )
            local.upsertProfile(updatedProfile)
        }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        val signOutFailure = runCatching {
            remote.signOut()
        }.exceptionOrNull()

        realtimeJobs.values.forEach { it.cancel() }
        realtimeJobs.clear()
        local.clearAllData()

        if (signOutFailure != null) {
            throw signOutFailure
        }
    }

    override suspend fun startChat(username: String): String = withContext(Dispatchers.IO) {
        val myId = remote.getCurrentUserId() ?: error("Not authenticated")
        val targetUser = remote.searchUser(username) ?: error("User not found: @$username")
        
        val chatId = remote.createChat(myId, targetUser.id)
        
        // Optimistically insert into local DB
        local.upsertProfile(targetUser.toDomain())
        local.upsertChat(
            Chat(
                id = chatId,
                participant = targetUser.toDomain(),
                createdAt = Clock.System.now()
            )
        )
        chatId
    }

    // ─── Chats ────────────────────────────────────────────────────────────────

    override fun observeChats(): Flow<List<Chat>> = local.observeChats()

    override suspend fun markChatAsRead(chatId: String) = withContext(Dispatchers.IO) {
        local.markChatAsRead(chatId)
    }

    // ─── Messages ─────────────────────────────────────────────────────────────

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        local.observeMessages(chatId)

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

    private val realtimeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    override suspend fun syncMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                val remoteMessages = remote.fetchMessages(chatId)
                remoteMessages.forEach { dto ->
                    local.upsertMessage(dto.toDomain())
                }
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

    override suspend fun subscribeToRealtimeMessages(chatId: String) {
        if (realtimeJobs.containsKey(chatId)) return

        val job = scope.launch {
            remote.subscribeToMessages(chatId).collect { dto ->
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

    private fun readFileBytes(path: String): ByteArray {
        // Platform specific implementation needed
        return ByteArray(0) 
    }
}
