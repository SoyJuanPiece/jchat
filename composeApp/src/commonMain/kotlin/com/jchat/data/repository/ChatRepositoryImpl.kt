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

        // 3. Background remote send (Simulado)
        scope.launch {
            kotlinx.coroutines.delay(1000L) // Simular el tiempo de red
            local.updateMessageStatus(
                messageId = message.id,
                status = MessageStatus.SENT,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )

            // Simular respuesta del otro usuario (Bot)
            kotlinx.coroutines.delay(2000L)
            simulateIncomingMessage(chatId, "¡Hola! Soy un bot. Recibí tu mensaje: \"$content\"")
        }
    }

    private suspend fun simulateIncomingMessage(chatId: String, content: String) {
        val chat = local.observeChats().take(1).collect { chats ->
            val targetChat = chats.find { it.id == chatId }
            if (targetChat != null) {
                val now = Clock.System.now()
                val incomingMessage = Message(
                    id = uuid4().toString(),
                    chatId = chatId,
                    senderId = targetChat.participant.id, // Responder como el otro participante
                    content = content,
                    contentType = ContentType.TEXT,
                    status = MessageStatus.SENT,
                    createdAt = now,
                    updatedAt = now,
                )
                withContext(Dispatchers.IO) {
                    local.insertMessage(incomingMessage)
                    local.updateChatLastMessage(chatId, content, now.toEpochMilliseconds())
                }
            }
        }
    }

    /**
     * Uploads a media file and sends the message once the URL is available.
     * Emits upload progress [0.0 – 1.0] through the returned [Flow].
     *
     * The `onProgress` callback in [RemoteDataSource.uploadMedia] runs synchronously
     * on the upload thread. Since we are already inside a `flow { }` builder here,
     * we can call `emit()` directly from within that same coroutine context.
     */
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

        // Insert placeholder message locally
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

        // Simular la carga de archivos
        emit(0.0f)
        kotlinx.coroutines.delay(500L)
        emit(0.5f)
        kotlinx.coroutines.delay(500L)

        val publicUrl = "https://via.placeholder.com/150/0000FF/FFFFFF?text=${contentType.name}" // URL de prueba

        // Update local record with the remote URL
        withContext(Dispatchers.IO) {
            local.updateMessageMediaUrl(
                messageId = messageId,
                mediaUrl = publicUrl,
                status = MessageStatus.SENT,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
        emit(1.0f)
    }

    override suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        local.softDeleteMessage(messageId, Clock.System.now().toEpochMilliseconds())
    }

    // ─── Sync (Simulado) ─────────────────────────────────────────────────────────────────

    override suspend fun syncMessages(chatId: String) {
        // No-op: No se sincroniza con el backend por ahora.
        kotlinx.coroutines.delay(500L) // Simular un breve retraso
        Unit
    }

    override suspend fun subscribeToRealtimeMessages(chatId: String) {
        // No-op: No hay suscripción en tiempo real por ahora.
        kotlinx.coroutines.delay(100L)
    }

    override suspend fun unsubscribeFromRealtimeMessages(chatId: String) {
        // No-op: No hay desuscripción en tiempo real por ahora.
        kotlinx.coroutines.delay(100L)
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
