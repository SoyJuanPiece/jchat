package com.jchat.data.remote

import com.jchat.domain.model.ContentType
import com.jchat.domain.model.Message
import com.jchat.domain.model.MessageStatus
import com.jchat.domain.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject

// Private Json instance for manual decoding
private val json = Json { ignoreUnknownKeys = true }

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

    /** A Flow that emits the current user's ID whenever the auth session changes. */
    val authSessionFlow: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.id
    }

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserEmail(): String? = supabase.auth.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String) {
        // En 3.x signInWith es un método de Auth, no necesita import de extensión
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String, username: String, displayName: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("username", username)
                put("display_name", displayName)
            }
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    // ─── Profiles ─────────────────────────────────────────────────────────────

    suspend fun fetchProfile(userId: String): ProfileDto? =
        supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<ProfileDto>()

    suspend fun fetchCurrentUserProfile(): ProfileDto? {
        val userId = getCurrentUserId() ?: return null
        return fetchProfile(userId)
    }

    suspend fun updateProfile(userId: String, displayName: String, avatarUrl: String?) {
        supabase.from("profiles").update(
            {
                set("display_name", displayName)
                if (avatarUrl != null) {
                    set("avatar_url", avatarUrl)
                }
            }
        ) {
            filter { eq("id", userId) }
        }
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

    suspend fun sendMessage(message: MessageDto): MessageDto =
        supabase.from("messages")
            .insert(message) { select() }
            .decodeSingle<MessageDto>()

    fun subscribeToMessages(chatId: String): Flow<MessageDto> = flow {
        val channel = supabase.channel("messages:$chatId")
        activeChannels[chatId] = channel
        channel.subscribe()
        supabase.realtime.connect()

        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }.collect { action ->
            val jsonString = action.record.toString()
            val dto = json.decodeFromString(MessageDto.serializer(), jsonString)
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

    fun uploadMedia(
        bucket: String,
        remotePath: String,
        data: ByteArray,
    ): Flow<String> = flow {
        val storageRef = supabase.storage[bucket]
        storageRef.upload(remotePath, data) { upsert = true }
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
