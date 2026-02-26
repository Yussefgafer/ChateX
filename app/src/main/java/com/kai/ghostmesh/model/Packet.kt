package com.kai.ghostmesh.model

import java.util.UUID

data class Packet(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val receiverId: String = "ALL", // "ALL" for broadcast, or specific peer ID
    val type: PacketType,
    val payload: String,
    val hopCount: Int = 3, // Max hops before packet dies
    val timestamp: Long = System.currentTimeMillis()
)

enum class PacketType {
    CHAT,
    PROFILE_SYNC,
    ACK // Acknowledgement (Future)
}
