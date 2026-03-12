package com.jchat.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val status: OnlineStatus = OnlineStatus.OFFLINE,
    val lastSeenAt: Instant? = null,
    val createdAt: Instant,
)

@Serializable
enum class OnlineStatus {
    ONLINE,
    AWAY,
    OFFLINE;

    companion object {
        fun fromString(value: String): OnlineStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OFFLINE
    }
}
