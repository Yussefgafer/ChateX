package com.kai.ghostmesh.features.chat

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.model.Message
import com.kai.ghostmesh.core.ui.components.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RenderEffect
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(peerName, fontWeight = FontWeight.Bold)
                        if (isTyping) {
                            Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(transportType ?: "Disconnected", style = MaterialTheme.typography.labelSmall)
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
                }
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
                onVoiceStop = onStopVoice
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(24.dp)
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

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconButton(onClick = onAttach) { Icon(Icons.Default.Add, null) }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                placeholder = { Text("Write a message...") },
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (text.isBlank()) {
                ExpressiveIconButton(onClick = onVoiceStart) { Icon(Icons.Default.Mic, null) }
            } else {
                ExpressiveIconButton(onClick = onSend) { Icon(Icons.AutoMirrored.Filled.Send, null) }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, onDelete: () -> Unit, onReply: () -> Unit) {
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    // Calculate burn/dissolve progress
    val burnProgress = if (message.isSelfDestruct && message.expiryTime > 0) {
        val remaining = (message.expiryTime - System.currentTimeMillis()).coerceAtLeast(0)
        val total = 60000f // Assume 60s for demo or map correctly
        (1f - (remaining / total)).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = color,
            onClick = onReply,
            modifier = Modifier.graphicsLayer {
                if (burnProgress > 0.05f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val shader = GhostShaders.createDissolveShader(burnProgress, size.width, size.height)
                    if (shader is android.graphics.RuntimeShader) {
                        renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "child").asComposeRenderEffect()
                    }
                }
            }
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
