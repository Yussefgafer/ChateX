package com.kai.ghostmesh.model

import java.util.UUID

data class UserProfile(
    val id: String = "",
    val name: String = "Unknown User",
    val status: String = "Roaming the void",
    val color: Int = 0xFF00FF7F.toInt()
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
    val timestamp: Long = System.currentTimeMillis()
)

enum class PacketType {
    CHAT,
    IMAGE,
    VOICE, // 🚀 New!
    PROFILE_SYNC,
    ACK,
    TYPING_START,
    TYPING_STOP
}

data class Message(
    val id: String = "",
    val sender: String, 
    val content: String, 
    val isMe: Boolean,
    val isImage: Boolean = false,
    val isVoice: Boolean = false, // 🚀 New!
    val isSelfDestruct: Boolean = false,
    val expiryTime: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val hopsTaken: Int = 0
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}
