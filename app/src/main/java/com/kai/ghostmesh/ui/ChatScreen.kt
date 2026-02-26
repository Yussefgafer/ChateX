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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
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
    
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSendImage(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ghostName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Spectral Session Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages, key = { it.timestamp }) { msg ->
                    SpectralMessageBubble(msg)
                }
            }
        }

        // Input Bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { imageLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                }
                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Cast a spell...", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    )
                )
                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSendMessage(textState)
                            textState = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun SpectralMessageBubble(msg: Message) {
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val glowColor = if (msg.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (msg.isMe) 20.dp else 4.dp,
        bottomEnd = if (msg.isMe) 4.dp else 20.dp
    )
    
    var isBlurred by remember { mutableStateOf(msg.isImage) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            modifier = Modifier.spectralGlow(glowColor, radius = 12.dp, shape = shape),
            color = Color.White.copy(alpha = 0.05f),
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, glowColor.copy(alpha = 0.3f))
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
                                .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .blur(if (isBlurred) 20.dp else 0.dp)
                                .clickable { isBlurred = !isBlurred },
                            contentScale = ContentScale.Crop
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
            val remaining = (msg.expiryTime - System.currentTimeMillis()) / 1000
            Text(
                "Vanishing in ${remaining.coerceAtLeast(0)}s...", 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.Red.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
