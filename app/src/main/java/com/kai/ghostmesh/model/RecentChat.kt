package com.kai.ghostmesh.model

data class RecentChat(
    val profile: UserProfile,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0
)
