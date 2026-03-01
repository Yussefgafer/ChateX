package com.kai.ghostmesh.features.discovery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.features.chat.ChatScreen
import com.kai.ghostmesh.features.chat.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DiscoveryHub(
    connectedNodes: Map<String, UserProfile>,
    routingTable: Map<String, com.kai.ghostmesh.core.mesh.Route>,
    meshHealth: Int,
    chatViewModel: ChatViewModel,
    cornerRadius: Int = 16,
    onShout: (String) -> Unit
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val chatHistory by chatViewModel.messages.collectAsState()
    val typingGhosts by chatViewModel.typingGhosts.collectAsState()
    val replyToMessage by chatViewModel.replyToMessage.collectAsState()
    val scope = rememberCoroutineScope()

    var shoutText by remember { mutableStateOf("") }
    var showShoutDialog by remember { mutableStateOf(false) }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DISCOVERY HUB", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Text("${connectedNodes.size} GHOSTS ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showShoutDialog = true }) {
                                Icon(Icons.Default.FlashOn, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    DiscoveryList(
                        nodes = connectedNodes,
                        routingTable = routingTable,
                        onNodeClick = { id, name ->
                            chatViewModel.setActiveChat(id)
                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        detailPane = {
            val ghostId = navigator.currentDestination?.contentKey
            if (ghostId != null) {
                val ghostName = connectedNodes[ghostId]?.name ?: "Unknown"
                ChatScreen(
                    ghostId = ghostId,
                    ghostName = ghostName,
                    messages = chatHistory,
                    isTyping = typingGhosts.contains(ghostId),
                    onSendMessage = { chatViewModel.sendMessage(it, true, 0, 3, UserProfile(name = "Me")) },
                    onSendImage = { },
                    onStartVoice = { },
                    onStopVoice = { },
                    onPlayVoice = { },
                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                    onTypingChange = { chatViewModel.sendTyping(it, UserProfile(name = "Me")) },
                    onBack = {
                        chatViewModel.setActiveChat(null)
                        scope.launch { navigator.navigateBack() }
                    },
                    replyToMessage = replyToMessage,
                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                    onClearReply = { chatViewModel.clearReply() },
                    cornerRadius = cornerRadius
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a Ghost to start communicating", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    )

    if (showShoutDialog) {
        AlertDialog(
            onDismissRequest = { showShoutDialog = false },
            title = { Text("GLOBAL SHOUT") },
            text = {
                OutlinedTextField(
                    value = shoutText,
                    onValueChange = { shoutText = it },
                    placeholder = { Text("Blast a message to all nodes...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { onShout(shoutText); shoutText = ""; showShoutDialog = false }) {
                    Text("SHOUT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShoutDialog = false }) { Text("CANCEL") }
            },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }
}
