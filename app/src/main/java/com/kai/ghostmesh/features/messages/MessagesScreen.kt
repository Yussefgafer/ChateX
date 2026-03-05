package com.kai.ghostmesh.features.messages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.RecentChat
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.core.ui.theme.GhostMotion
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    chats: List<RecentChat>,
    meshHealth: Int,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit,
    cornerRadius: Int = 28
) {
    var searchQuery by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var interactingIndex by remember { mutableStateOf(-1) }

    val filteredChats = remember(searchQuery, chats) {
        if (searchQuery.isEmpty()) chats
        else chats.filter { it.profile.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                MorphingDiscoveryButton(onClick = onNavigateToRadar)
            },
            topBar = {
                Column {
                    MediumTopAppBar(
                        title = {
                            Text(
                                "PEER MESH",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        },
                        navigationIcon = {
                            ExpressiveIconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            ExpressiveIconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )

                    Box(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                        SearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onSearch = { active = false },
                                    expanded = active,
                                    onExpandedChange = { active = it },
                                    placeholder = { Text("Search mesh network...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            ExpressiveIconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                )
                            },
                            expanded = active,
                            onExpandedChange = { active = it },
                            modifier = Modifier.fillMaxWidth().physicalTilt(),
                            shape = RoundedCornerShape(cornerRadius.dp),
                            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
                        ) {
                            LazyColumn {
                                itemsIndexed(filteredChats) { _, chat ->
                                    RecentChatItem(
                                        chat = chat,
                                        isInteracting = false,
                                        userRadius = cornerRadius,
                                        onClick = { onNavigateToChat(chat.profile.id, chat.profile.name) }
                                    )
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
                        contentPadding = PaddingValues(bottom = 120.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(filteredChats, key = { _, chat -> chat.profile.id }) { index, chat ->
                            val isNeighborInteracting = interactingIndex != -1 && interactingIndex != index
                            RecentChatItem(
                                chat = chat,
                                isInteracting = interactingIndex == index,
                                modifier = Modifier.proximityDisplacement(isNeighborInteracting),
                                onInteracting = { interactingIndex = if (it) index else -1 },
                                onClick = { onNavigateToChat(chat.profile.id, chat.profile.name) },
                                userRadius = cornerRadius
                            )
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp)) {
            Icon(
                imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.CloudQueue,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isSearching) "NO NODES FOUND" else "MESH IS SILENT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun RecentChatItem(
    chat: RecentChat,
    isInteracting: Boolean,
    modifier: Modifier = Modifier,
    onInteracting: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    userRadius: Int
) {
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

    val dynamicRadius by animateDpAsState(
        targetValue = if (isInteracting) (userRadius / 2).dp else userRadius.dp,
        animationSpec = GhostMotion.MassSpringDp,
        label = "corner_morph"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(dynamicRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .jellyClickable(
                onClick = onClick,
                onLongClick = { onInteracting(true) }
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(dynamicRadius))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color(chat.profile.color).copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp).border(1.dp, Color(chat.profile.color).copy(alpha = 0.3f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            chat.profile.name.take(1).uppercase(),
                            color = Color(chat.profile.color),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(chat.profile.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    chat.lastMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                        Text(chat.unreadCount.toString(), color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
