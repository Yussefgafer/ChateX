package com.kai.ghostmesh.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class RecentChat(
    val profile: UserProfile,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0
)
