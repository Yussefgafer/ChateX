package com.kai.ghostmesh.model

import java.util.UUID

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
