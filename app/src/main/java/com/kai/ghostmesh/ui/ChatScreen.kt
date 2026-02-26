package com.kai.ghostmesh.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.model.Message
import com.kai.ghostmesh.model.MessageStatus
import com.kai.ghostmesh.ui.components.spectralGlow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    ghostId: String,
    ghostName: String,
    messages: List<Message>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onSendImage: (android.net.Uri) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onSendImage(it) } }

    LaunchedEffect(textState) {
        onTypingChange(textState.isNotBlank())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ghostName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        AnimatedVisibility(visible = isTyping) {
                            Text("Spectral activity detected...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.padding(8.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imageLauncher.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary) }
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Summon message...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                    )
                    IconButton(onClick = { if (textState.isNotBlank()) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSendMessage(textState); textState = "" } }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { msg -> SpectralMessageBubble(msg) }
        }
    }
}

@Composable
fun SpectralMessageBubble(msg: Message) {
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(color = bubbleColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 280.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (msg.isImage) {
                    val bitmap = remember(msg.content) {
                        try {
                            val bytes = Base64.decode(msg.content, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) { null }
                    }
                    bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Inside) }
                } else {
                    Text(text = msg.content, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
                
                Row(modifier = Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (msg.hopsTaken > 0) {
                        Text("${msg.hopsTaken} hops", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                    }
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                    Text(time, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    if (msg.isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (msg.status == MessageStatus.DELIVERED) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (msg.status == MessageStatus.DELIVERED) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
