package com.jchat.data.remote

import com.jchat.domain.model.ContentType
import com.jchat.domain.model.Message
import com.jchat.domain.model.MessageStatus
import com.jchat.domain.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Remote DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("content") val content: String? = null,
    @SerialName("content_type") val contentType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("status") val status: String = "sent",
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("status") val status: String = "offline",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

// ─── Remote Data Source ───────────────────────────────────────────────────────

/**
 * Handles all network communication with the Supabase backend.
 */
class RemoteDataSource(private val supabase: SupabaseClient) {

    private val activeChannels = mutableMapOf<String, RealtimeChannel>()

    // ─── Auth ─────────────────────────────────────────────────────────────────

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // ─── Profiles ─────────────────────────────────────────────────────────────

    suspend fun fetchProfile(userId: String): ProfileDto? =
        supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<ProfileDto>()

    suspend fun fetchCurrentUserProfile(): ProfileDto? {
        val userId = getCurrentUserId() ?: return null
        return fetchProfile(userId)
    }

    // ─── Messages ─────────────────────────────────────────────────────────────

    suspend fun fetchMessages(chatId: String, limit: Int = 50): List<MessageDto> =
        supabase.from("messages")
            .select {
                filter { eq("chat_id", chatId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                this.limit(limit.toLong())
            }
            .decodeList<MessageDto>()

    /**
     * Sends a message to Supabase and returns the persisted [MessageDto].
     */
    suspend fun sendMessage(message: MessageDto): MessageDto =
        supabase.from("messages")
            .insert(message) { select() }
            .decodeSingle<MessageDto>()

    /**
     * Subscribes to real-time INSERT events for [chatId].
     * Returns a [Flow] of incoming [MessageDto]s from the Realtime channel.
     */
    fun subscribeToMessages(chatId: String): Flow<MessageDto> = flow {
        val channel = supabase.channel("messages:$chatId")
        activeChannels[chatId] = channel

        // Subscribe to the channel before connecting so that the listener
        // is registered before any incoming events can arrive.
        channel.subscribe()
        supabase.realtime.connect()

        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }.collect { action ->
            val dto = action.decode<MessageDto>()
            emit(dto)
        }
    }

    suspend fun unsubscribeFromMessages(chatId: String) {
        activeChannels[chatId]?.let { channel ->
            channel.unsubscribe()
            activeChannels.remove(chatId)
        }
    }

    // ─── Storage ──────────────────────────────────────────────────────────────

    /**
     * Uploads a local file to Supabase Storage and reports progress via a [Flow].
     *
     * @param bucket Storage bucket name (e.g., "chat-media").
     * @param remotePath Destination path within the bucket.
     * @param data Raw bytes of the file.
     * @return A [Flow] that emits the public URL of the uploaded file when complete.
     */
    fun uploadMedia(
        bucket: String,
        remotePath: String,
        data: ByteArray,
    ): Flow<String> = flow {
        val storageRef = supabase.storage[bucket]
        storageRef.upload(remotePath, data) {
            upsert = true
        }
        val publicUrl = storageRef.publicUrl(remotePath)
        emit(publicUrl)
    }
}

// ─── DTO → Domain Mappers ────────────────────────────────────────────────────

fun MessageDto.toDomain(): Message = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    content = content,
    contentType = ContentType.fromString(contentType),
    mediaUrl = mediaUrl,
    status = MessageStatus.fromString(status),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

fun ProfileDto.toDomain(): Profile = Profile(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    status = com.jchat.domain.model.OnlineStatus.fromString(status),
    lastSeenAt = lastSeenAt?.let { Instant.parse(it) },
    createdAt = Instant.parse(createdAt),
)
