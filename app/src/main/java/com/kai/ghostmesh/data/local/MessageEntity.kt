package com.kai.ghostmesh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kai.ghostmesh.model.MessageStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val ghostId: String,
    val senderName: String,
    val content: String,
    val isMe: Boolean,
    val isImage: Boolean = false,
    val isSelfDestruct: Boolean = false,
    val expiryTime: Long = 0,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val hopsTaken: Int = 0 // 🚀 Persist diagnostic info
)
