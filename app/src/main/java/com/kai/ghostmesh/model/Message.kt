package com.kai.ghostmesh.model

data class UserProfile(
    val id: String = "",
    val name: String = "Unknown User",
    val status: String = "Roaming the void",
    val color: Int = 0xFF00FF7F.toInt() // Default Ectoplasm Green
)

data class Message(
    val sender: String, 
    val content: String, 
    val isMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
