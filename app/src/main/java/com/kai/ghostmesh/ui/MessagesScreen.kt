package com.kai.ghostmesh.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.model.RecentChat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    recentChats: List<RecentChat>,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredChats = remember(searchQuery, recentChats) {
        if (searchQuery.isBlank()) recentChats
        else recentChats.filter { it.profile.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("ARCHIVES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Find spectral data...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(0.dp), // SHARP design
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
            }
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateToRadar) { Icon(Icons.Default.Radar, "Radar", tint = Color.Gray) }
                        IconButton(onClick = { }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) { 
                            Icon(Icons.Default.ChatBubble, "Archives", tint = Color.Black) 
                        }
                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.Gray) }
                    }
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (filteredChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                        Text(if (searchQuery.isEmpty()) "THE VOID IS SILENT" else "NO SPECTRAL MATCH", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChats, key = { it.profile.id }) { chat ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visible,
                            enter = slideInHorizontally() + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        ) {
                            RecentChatItem(chat) { onNavigateToChat(chat.profile.id, chat.profile.name) }
                        }
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
