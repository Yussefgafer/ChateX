package com.kai.ghostmesh.features.chat

import android.net.Uri
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    ghostId: String,
    ghostName: String,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ghostName, fontWeight = FontWeight.Bold)
                        if (isTyping) {
                            Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(transportType ?: "Disconnected", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    ExpressiveIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
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
            contentPadding = PaddingValues(16.dp)
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
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconButton(onClick = onAttach) { Icon(Icons.Default.Add, null) }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Spectral message...") },
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
    
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = color,
            onClick = onReply
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
