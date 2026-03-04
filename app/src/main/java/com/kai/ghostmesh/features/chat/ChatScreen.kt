package com.kai.ghostmesh.features.chat

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.model.Message
import com.kai.ghostmesh.core.model.MessageStatus
import com.kai.ghostmesh.core.ui.components.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: String,
    peerName: String,
    messages: List<Message>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit,
    onSendVideo: (Uri) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    replyToMessage: ChatViewModel.ReplyInfo?,
    onSetReply: (String, String, String) -> Unit,
    onClearReply: () -> Unit,
    cornerRadius: Int,
    transportType: String?
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSendImage(it) }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSendVideo(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MediumTopAppBar(
                    title = {
                        Column {
                            Text(peerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            if (isTyping) {
                                Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text(transportType ?: "Disconnected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    navigationIcon = {
                        ExpressiveIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    actions = {
                        ExpressiveIconButton(onClick = { videoLauncher.launch("video/*") }) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Send Video")
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                ChatInput(
                    text = textState,
                    onTextChange = {
                        textState = it
                        onTypingChange(it.isNotBlank())
                    },
                    onSend = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState)
                            textState = ""
                            onTypingChange(false)
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    },
                    onAttach = { imageLauncher.launch("image/*") },
                    onVoiceStart = onStartVoice,
                    onVoiceStop = onStopVoice,
                    replyToMessage = replyToMessage,
                    onClearReply = onClearReply
                )
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onDelete = { onDeleteMessage(msg.id) },
                        onReply = { onSetReply(msg.id, msg.content, msg.sender) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit,
    replyToMessage: ChatViewModel.ReplyInfo?,
    onClearReply: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))) {
        AnimatedVisibility(visible = replyToMessage != null) {
            replyToMessage?.let {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(it.senderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(it.messageContent, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                    ExpressiveIconButton(onClick = onClearReply) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                }
            }
        }

        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconButton(onClick = onAttach) { Icon(Icons.Default.Add, null) }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).physicalTilt(),
                placeholder = { Text("Write a message...") },
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )

            if (text.isBlank()) {
                ExpressiveIconButton(onClick = onVoiceStart) { Icon(Icons.Default.Mic, null) }
            } else {
                ExpressiveIconButton(onClick = onSend, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, onDelete: () -> Unit, onReply: () -> Unit) {
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    // Entry Animation: Subtle pop-up bounce
    val scale by animateFloatAsState(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "entry_pop"
    )

    // Delivery Bounce: Triggers when status changes
    val bounceByStatus by animateFloatAsState(
        targetValue = if (message.status != MessageStatus.SENT) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "status_bounce"
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer {
            scaleX = scale * if (message.status != MessageStatus.SENT) bounceByStatus else 1f
            scaleY = scale * if (message.status != MessageStatus.SENT) bounceByStatus else 1f
        },
        contentAlignment = alignment
    ) {
        GlassCard(
            onClick = onReply,
            containerColor = color,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column {
                if (!message.isMe) {
                    Text(message.sender, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    if (message.isEncrypted && !message.decryptionFailed) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(12.dp).alpha(0.6f))
                        Spacer(Modifier.width(4.dp))
                    } else if (message.decryptionFailed) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }

                    Icon(
                        imageVector = when(message.status) {
                            MessageStatus.SENT -> Icons.Default.Check
                            MessageStatus.DELIVERED -> Icons.Default.DoneAll
                            MessageStatus.READ -> Icons.Default.DoneAll
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (message.status == MessageStatus.READ) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
