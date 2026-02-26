package com.kai.ghostmesh.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.model.Message
import com.kai.ghostmesh.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
private fun formatSmartTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
        diff < TimeUnit.DAYS.toMillis(2) -> "yesterday"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    ghostId: String,
    ghostName: String,
    messages: List<Message>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onSendImage: (android.net.Uri) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> uri?.let { onSendImage(it) } }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(textState) { onTypingChange(textState.isNotBlank()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ghostName.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        androidx.compose.animation.AnimatedVisibility(visible = isTyping) {
                            Text("RECV PACKETS...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imageLauncher.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary) }
                    OutlinedTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("COMMAND...", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(0.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    if (textState.isBlank()) {
                        IconButton(
                            modifier = Modifier.pointerInput(Unit) { 
                                detectTapGestures(onPress = { 
                                    try { isRecording = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStartVoice(); awaitRelease() } finally { isRecording = false; onStopVoice() } 
                                }) 
                            }, 
                            onClick = {}
                        ) {
                            Icon(if (isRecording) Icons.Default.MicNone else Icons.Default.Mic, null, tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSendMessage(textState); textState = "" }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), 
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                SpectralMessageBubble(msg, onPlayVoice, onDeleteMessage)
            }
        }
    }
}

@Composable
fun SpectralMessageBubble(msg: Message, onPlayVoice: (String) -> Unit, onDelete: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f)
    val borderColor = if (msg.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(if (msg.isMe) 8.dp else 0.dp),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .pointerInput(Unit) { detectTapGestures(onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showMenu = true }) }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when {
                    msg.isImage -> {
                        var isBlurred by remember { mutableStateOf(true) }
                        val bitmap = remember(msg.content) {
                            try {
                                val bytes = Base64.decode(msg.content, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        bitmap?.let { 
                            Image(
                                bitmap = it.asImageBitmap(), 
                                contentDescription = null, 
                                modifier = Modifier
                                    .clip(RoundedCornerShape(0.dp))
                                    .blur(if (isBlurred) 40.dp else 0.dp)
                                    .clickable { isBlurred = !isBlurred }, 
                                contentScale = ContentScale.Inside
                            ) 
                        }
                    }
                    msg.isVoice -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onPlayVoice(msg.content) }) {
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("SPECTRAL_DATA.wav", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                    else -> { Text(text = msg.content, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val time = formatSmartTime(msg.timestamp)
                    Text(time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    if (msg.isMe) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when(msg.status) {
                                    MessageStatus.SENT -> "sent"
                                    MessageStatus.DELIVERED -> "delivered"
                                    MessageStatus.READ -> "read"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.status == MessageStatus.READ) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = when(msg.status) {
                                    MessageStatus.SENT -> Icons.Default.Check
                                    MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                    MessageStatus.READ -> Icons.Default.DoneAll
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (msg.status == MessageStatus.READ) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("PURGE DATA") }, 
                onClick = { onDelete(msg.id); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            )
        }
    }
}
