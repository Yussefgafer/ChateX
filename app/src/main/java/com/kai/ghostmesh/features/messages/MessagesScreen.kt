package com.kai.ghostmesh.features.messages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    cornerRadius: Int,
    typingPeers: Set<String> = emptySet()
) {
    val scrollState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MediumTopAppBar(
                    title = {
                        Column {
                            Text("CHATE-X", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium, letterSpacing = 2.sp)
                            Text("ENCRYPTED MESH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        ExpressiveIconButton(onClick = onNavigateToRadar) { Icon(Icons.Default.Radar, null) }
                        ExpressiveIconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, null) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (chats.isEmpty()) {
                    item { EmptyChatsState() }
                } else {
                    items(chats, key = { it.profile.id }) { chat ->
                        val isTyping = chat.profile.id in typingPeers

                        // Adaptive radius based on velocity
                        var previousOffset by remember { mutableIntStateOf(0) }
                        val velocity = kotlin.math.abs(scrollState.firstVisibleItemScrollOffset - previousOffset)
                        LaunchedEffect(scrollState.firstVisibleItemScrollOffset) { previousOffset = scrollState.firstVisibleItemScrollOffset }

                        val absVelocity = velocity.coerceAtMost(120)
                        val dynamicRadius = (cornerRadius - (absVelocity * 0.18f)).coerceAtLeast(12f)

                        RecentChatRow(
                            chat = chat,
                            isTyping = isTyping,
                            onClick = { onNavigateToChat(chat.profile.id, chat.profile.name) },
                            modifier = Modifier.clip(RoundedCornerShape(dynamicRadius.dp)),
                            userRadius = dynamicRadius.toInt()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentChatRow(
    chat: RecentChat,
    isTyping: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    userRadius: Int
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(userRadius.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .jellyClickable(onClick = onClick)
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(userRadius.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatAvatar(chat.profile.name, chat.profile.color)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(chat.profile.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(chat.lastMessageTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTyping) {
                        Text("typing...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    } else {
                        Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (chat.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(chat.unreadCount.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatAvatar(name: String, color: Int) {
    Surface(
        shape = CircleShape,
        color = Color(color).copy(alpha = 0.15f),
        modifier = Modifier.size(56.dp).border(1.dp, Color(color).copy(alpha = 0.3f), CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, color = Color(color), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun EmptyChatsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(64.dp).alpha(0.2f))
        Spacer(Modifier.height(24.dp))
        Text("NO ACTIVE SECURE SESSIONS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
