package com.kai.ghostmesh.core.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class UserProfile(
    val id: String = "",
    val name: String = "Unknown User",
    val status: String = "Roaming the void",
    val color: Int = 0xFF00FF7F.toInt(),
    val profileImage: String? = null,
    val isOnline: Boolean = false,
    val batteryLevel: Int = 100,
    val bestEndpoint: String? = null
)

@Immutable
data class Packet(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val receiverId: String = "ALL",
    val type: PacketType,
    val payload: String,
    val hopCount: Int = 3,
    val isSelfDestruct: Boolean = false,
    val expirySeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSender: String? = null,
    val senderBattery: Int = 100,
    val pathCost: Float = 0f
)

@Immutable
data class Message(
    val id: String = "",
    val sender: String, 
    val content: String, 
    val isMe: Boolean,
    val isImage: Boolean = false,
    val isVoice: Boolean = false,
    val isSelfDestruct: Boolean = false,
    val expiryTime: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val hopsTaken: Int = 0,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSender: String? = null,
    val reactions: Map<String, String> = emptyMap()
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}

enum class PacketType {
    CHAT, IMAGE, VOICE, FILE, ACK, KEY_EXCHANGE, PROFILE_SYNC, TYPING_START, TYPING_STOP, LAST_SEEN, PROFILE_IMAGE, GATEWAY_AVAILABLE,
    TUNNEL, LINK_STATE, BATTERY_HEARTBEAT, REACTION
}

fun Packet.isValid(): Boolean {
    // Basic null and blank checks
    @Suppress("SENSELESS_COMPARISON")
    if (senderId == null || senderId.isBlank()) return false
    if (id == null || id.isBlank()) return false
    if (payload == null) return false

    // Anti-replay protection
    val now = System.currentTimeMillis()
    if (timestamp > now + 300_000L) return false // Future: Max 5 mins
    if (timestamp < now - 3_600_000L) return false // Stale: Max 1 hour

    // Security: Strict hop count bounds
    if (hopCount < 0 || hopCount > 10) return false

    // Security: Payload size sanity (100KB limit)
    if (payload.length > 102400) return false

    return true
}
