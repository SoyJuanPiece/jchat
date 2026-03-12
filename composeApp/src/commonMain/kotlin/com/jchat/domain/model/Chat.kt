package com.jchat.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String,
    val participant: Profile,
    val lastMessagePreview: String? = null,
    val lastMessageAt: Instant? = null,
    val unreadCount: Int = 0,
    val createdAt: Instant,
)
