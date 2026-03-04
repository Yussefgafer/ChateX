package com.kai.ghostmesh.features.messages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.model.RecentChat
import com.kai.ghostmesh.core.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    chats: List<RecentChat>,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit,
    cornerRadius: Int = 28
) {
    var searchQuery by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    val filteredChats = remember(searchQuery, chats) {
        if (searchQuery.isEmpty()) chats
        else chats.filter { it.profile.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 3% Noise texture
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                MorphingDiscoveryButton(
                    onClick = onNavigateToRadar
                )
            },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "PEER MESH",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                        },
                        navigationIcon = {
                            ExpressiveIconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            ExpressiveIconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )

                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        SearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onSearch = { active = false },
                                    expanded = active,
                                    onExpandedChange = { active = it },
                                    placeholder = { Text("Search mesh network...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            ExpressiveIconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                                            }
                                        }
                                    }
                                )
                            },
                            expanded = active,
                            onExpandedChange = { active = it },
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
                        contentPadding = PaddingValues(bottom = 80.dp, start = 24.dp, end = 24.dp)
                    ) {
                        itemsIndexed(filteredChats, key = { _, chat -> chat.profile.id }) { index, chat ->
                            RecentChatItem(
                                chat = chat,
                                onClick = { onNavigateToChat(chat.profile.id, chat.profile.name) }
                            )
                            if (index < filteredChats.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "NO NODES FOUND" else "MESH IS SILENT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearching) "Try a different search term" else "Awaiting peer signals. Ensure transports are active in Advanced Configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        if (chat.lastMessageTime == 0L) return@remember "never"
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
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
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
