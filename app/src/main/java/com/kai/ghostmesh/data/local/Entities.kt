package com.kai.ghostmesh.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kai.ghostmesh.model.MessageStatus

@Entity(tableName = "messages", indices = [Index(value = ["ghostId"]), Index(value = ["timestamp"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val ghostId: String,
    val senderName: String,
    val content: String,
    val isMe: Boolean,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val metadata: String = "{}"
)

@Entity(tableName = "profiles", indices = [Index(value = ["lastSeen"])])
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val color: Int = 0xFF00FF7F.toInt(),
    val metadata: String = "{}"
)
