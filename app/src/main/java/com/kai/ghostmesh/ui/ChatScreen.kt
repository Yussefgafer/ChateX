package com.kai.ghostmesh.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import com.kai.ghostmesh.ui.components.spectralGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    ghostName: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onSendImage: (android.net.Uri) -> Unit,
    onBack: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSendImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ghostName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Active Spectral Link", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Summon message...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    FloatingActionButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendMessage(textState)
                                textState = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.timestamp }) { msg ->
                SpectralMessageBubble(msg)
            }
        }
    }
}

@Composable
fun SpectralMessageBubble(msg: Message) {
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val glowColor = if (msg.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val bubbleColor = if (msg.isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (msg.isMe) 16.dp else 4.dp,
        bottomEnd = if (msg.isMe) 4.dp else 16.dp
    )
    
    var isBlurred by remember { mutableStateOf(msg.isImage) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = Modifier.padding(if (msg.isImage) 4.dp else 12.dp)) {
                if (msg.isImage) {
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
                                .clip(RoundedCornerShape(12.dp))
                                .blur(if (isBlurred) 30.dp else 0.dp)
                                .clickable { isBlurred = !isBlurred },
                            contentScale = ContentScale.Inside
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (msg.isSelfDestruct) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = msg.content, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (msg.isSelfDestruct) {
            val remaining = ((msg.expiryTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            Text("Fading: ${remaining}s", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}
