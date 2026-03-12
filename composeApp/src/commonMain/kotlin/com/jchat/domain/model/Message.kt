package com.jchat.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String? = null,
    val contentType: ContentType = ContentType.TEXT,
    val mediaUrl: String? = null,
    val mediaLocalPath: String? = null,
    val status: MessageStatus = MessageStatus.SENDING,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean = false,
)

@Serializable
enum class ContentType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    FILE;

    companion object {
        fun fromString(value: String): ContentType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
    }
}

@Serializable
enum class MessageStatus {
    /** Message is queued locally, not yet sent to the server. */
    SENDING,

    /** Message was accepted by the server. */
    SENT,

    /** Message was delivered to the recipient's device. */
    DELIVERED,

    /** Message was read by the recipient. */
    READ,

    /** Delivery failed (will be retried). */
    FAILED;

    companion object {
        fun fromString(value: String): MessageStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SENDING
    }
}
