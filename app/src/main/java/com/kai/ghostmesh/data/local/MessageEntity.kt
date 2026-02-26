package com.kai.ghostmesh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ghostId: String,
    val senderName: String,
    val content: String,
    val isMe: Boolean,
    val isImage: Boolean = false, // 🚀 New!
    val isSelfDestruct: Boolean = false,
    val expiryTime: Long = 0,
    val timestamp: Long
)
