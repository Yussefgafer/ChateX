package com.kai.ghostmesh.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kai.ghostmesh.core.model.MessageStatus

@Entity(tableName = "messages", indices = [Index(value = ["ghostId"]), Index(value = ["timestamp"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val ghostId: String,
    val senderName: String,
    val content: String,
    val isMe: Boolean,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSender: String? = null,
    val metadata: String = "{}",
    val expiryTimestamp: Long = 0L,
    val isImage: Boolean = false,
    val isVoice: Boolean = false,
    val hopsTaken: Int = 0
)

@Entity(tableName = "profiles", indices = [Index(value = ["lastSeen"])])
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val color: Int = 0xFF00FF7F.toInt(),
    val profileImage: String? = null,
    val isOnline: Boolean = false,
    val batteryLevel: Int = 100,
    val bestEndpoint: String? = null,
    val metadata: String = "{}"
)
