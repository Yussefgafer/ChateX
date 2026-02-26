package com.kai.ghostmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.model.RecentChat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    recentChats: List<RecentChat>,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Spectral Archives", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), tonalElevation = 0.dp) {
                NavigationBarItem(selected = false, onClick = onNavigateToRadar, icon = { Icon(Icons.Default.Radar, null) }, label = { Text("Radar") })
                NavigationBarItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.ChatBubble, null) }, label = { Text("Archives") })
                NavigationBarItem(selected = false, onClick = onNavigateToSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Console") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            if (recentChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("The void is silent...", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recentChats, key = { it.profile.id }) { chat ->
                        RecentChatItem(chat) { onNavigateToChat(chat.profile.id, chat.profile.name) }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 72.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecentChatItem(chat: RecentChat, onClick: () -> Unit) {
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

    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(chat.profile.name, color = Color.White, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(chat.lastMessage, color = Color.Gray, maxLines = 1) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(chat.profile.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.profile.name.take(1).uppercase(), color = Color(chat.profile.color), style = MaterialTheme.typography.titleMedium)
            }
        },
        trailingContent = {
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
