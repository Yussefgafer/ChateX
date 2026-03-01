package com.kai.ghostmesh.features.messages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.RecentChat
import com.kai.ghostmesh.core.ui.components.physicalTilt
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    recentChats: List<RecentChat>,
    cornerRadius: Int = 16,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    
    val filteredChats = remember(searchQuery, recentChats) {
        if (searchQuery.isBlank()) recentChats
        else recentChats.filter { it.profile.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "MESH HUB",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "DECENTRALIZED NETWORK",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (recentChats.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { active = false },
                        active = active,
                        onActiveChange = { active = it },
                        placeholder = { Text("Search mesh...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cornerRadius.dp),
                        colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        LazyColumn {
                            itemsIndexed(filteredChats) { _, chat ->
                                RecentChatItem(chat) { onNavigateToChat(chat.profile.id, chat.profile.name) }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            if (filteredChats.isEmpty() && !active) {
                EmptyStateView(searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(filteredChats, key = { _, chat -> chat.profile.id }) { index, chat ->
                        RecentChatItem(
                            chat = chat,
                            onClick = { onNavigateToChat(chat.profile.id, chat.profile.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isSearching) "NO RESULTS" else "MESH IS QUIET",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearching) "Check your spelling" else "Awaiting connections...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentChatItem(chat: RecentChat, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val timeStr = remember(chat.lastMessageTime) {
        val now = System.currentTimeMillis()
        val diff = now - chat.lastMessageTime
        when {
            diff < 60000 -> "now"
            diff < 3600000 -> "${diff / 60000}m"
            diff < 86400000 -> "${diff / 3600000}h"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(chat.lastMessageTime))
        }
    }

    Surface(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick() 
        },
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .physicalTilt()
    ) {
        ListItem(
            headlineContent = { Text(chat.profile.name, fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text(
                    chat.lastMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(chat.profile.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        chat.profile.name.take(1).uppercase(),
                        color = Color(chat.profile.color),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    if (chat.unreadCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(chat.unreadCount.toString()) }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
