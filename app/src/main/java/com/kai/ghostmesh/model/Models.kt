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
    val payload: String, // Can be Text or Base64 Image
    val hopCount: Int = 3,
    val isSelfDestruct: Boolean = false,
    val expirySeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PacketType {
    CHAT,
    IMAGE, // 🚀 New Type
    PROFILE_SYNC,
    ACK
}

data class Message(
    val sender: String, 
    val content: String, 
    val isMe: Boolean,
    val isImage: Boolean = false, // 🚀 New flag
    val isSelfDestruct: Boolean = false,
    val expiryTime: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
