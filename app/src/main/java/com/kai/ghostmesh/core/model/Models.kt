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
    val profileImageLocalPath: String? = null,
    val isOnline: Boolean = false,
    val batteryLevel: Int = 100,
    val bestEndpoint: String? = null,
    val isProxied: Boolean = false,
    val gatewayId: String? = null,
    val secondaryRouteAvailable: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val reputation: Int = 0,
    val isMaster: Boolean = false
) {
    val reputationLevel: String
        get() = when {
            reputation > 1000 -> "VOICE OF THE VOID"
            reputation > 500 -> "ORACLE"
            reputation > 100 -> "GHOST"
            else -> "DRIFTER"
        }
}

@Immutable
data class NeighborInfo(
    val nodeId: String,
    val name: String,
    val batteryLevel: Int,
    val color: Int
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
    val pathCost: Float = 0f,
    val lamportTime: Long = 0,
    val reputation: Int = 0,
    val protocolVersion: Int = 2
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
    val reactions: Map<String, String> = emptyMap(),
    val lamportTime: Long = 0
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}

enum class PacketType {
    CHAT, IMAGE, VOICE, FILE, ACK, KEY_EXCHANGE, PROFILE_SYNC, TYPING_START, TYPING_STOP, LAST_SEEN, PROFILE_IMAGE, GATEWAY_AVAILABLE,
    TUNNEL, LINK_STATE, BATTERY_HEARTBEAT, REACTION, NEIGHBOR_LIST,
    TOPOLOGY_UPDATE, KEEP_ALIVE, BITFIELD, CHUNK_REQUEST, CHUNK_RESPONSE,
    CLUSTER_ELECTION, REPUTATION_SYNC, LOCATION_UPDATE
}

fun Packet.isValid(): Boolean {
    @Suppress("SENSELESS_COMPARISON")
    return senderId != null && senderId.isNotBlank()
}
