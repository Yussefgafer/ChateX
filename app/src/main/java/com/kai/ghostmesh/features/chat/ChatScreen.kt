package com.kai.ghostmesh.features.chat

import com.kai.ghostmesh.core.util.ImageUtils
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.core.model.Message
import com.kai.ghostmesh.core.model.MessageStatus
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.core.ui.theme.GhostMotion

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import android.util.Base64
import com.kai.ghostmesh.core.ui.components.AudioPlayer
import com.kai.ghostmesh.core.ui.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onStopVoicePlayback: () -> Unit,
    onDeleteMessage: (String) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    replyToMessage: ChatViewModel.ReplyInfo?,
    onSetReply: (String, String, String) -> Unit,
    onClearReply: () -> Unit,
    stagedMedia: List<ChatViewModel.StagedMedia>,
    onStageMedia: (Uri, ChatViewModel.MediaType) -> Unit,
    onUnstageMedia: (Uri) -> Unit,
    recordingDuration: Long,
    cornerRadius: Int,
    transportType: String?
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onStageMedia(it, ChatViewModel.MediaType.IMAGE) }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onStageMedia(it, ChatViewModel.MediaType.VIDEO) }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onStageMedia(it, ChatViewModel.MediaType.FILE) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MediumTopAppBar(
                    title = {
                        Column {
                            Text(peerName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                            if (isTyping) {
                                Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    reverseLayout = false,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val onMsgDelete = remember(message.id) { { _: String -> onDeleteMessage(message.id) } }
                        val onMsgPlayVoice = remember(message.content) { { _: String -> onPlayVoice(message.content) } }
                        val onMsgSetReply = remember(message.id, message.content, message.sender) { { _: String, _: String, _: String -> onSetReply(message.id, message.content, message.sender) } }

                        MessageBubble(
                            message = message,
                            onDelete = onMsgDelete,
                            onPlayVoice = onMsgPlayVoice,
                            onStopVoicePlayback = onStopVoicePlayback,
                            onSetReply = onMsgSetReply,
                            cornerRadius = cornerRadius
                        )
                    }
                }

                if (stagedMedia.isNotEmpty()) {
                    MediaStagingArea(stagedMedia, onUnstageMedia, cornerRadius)
                }

                if (replyToMessage != null) {
                    ReplyPreview(replyToMessage, onClearReply, cornerRadius)
                }

                ChatInput(
                    text = textState,
                    onTextChange = {
                        textState = it
                        onTypingChange(it.isNotEmpty())
                    },
                    onSend = {
                        onSendMessage(textState)
                        textState = ""
                        onTypingChange(false)
                    },
                    onAttach = { showAttachmentSheet = true },
                    onStartVoice = onStartVoice,
                    onStopVoice = onStopVoice,
                    recordingDuration = recordingDuration,
                    cornerRadius = cornerRadius
                )
            }
        }
    }

    if (showAttachmentSheet) {
        AttachmentSheet(
            onPhoto = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            onVideo = { videoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            onFile = { fileLauncher.launch("*/*") },
            onDismiss = { showAttachmentSheet = false },
            cornerRadius = cornerRadius
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onDelete: (String) -> Unit,
    onPlayVoice: (String) -> Unit,
    onStopVoicePlayback: () -> Unit,
    onSetReply: (String, String, String) -> Unit,
    cornerRadius: Int
) {
    val alignment = if (message.isMe) Alignment.End else Alignment.Start
    val containerColor = if (message.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (message.isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(cornerRadius.dp),
            color = containerColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onDelete(message.id) },
                        onDoubleTap = { onSetReply(message.id, message.content, message.sender) }
                    )
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.replyToId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(message.replyToSender ?: "Unknown", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(message.replyToContent ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }

                when {
                    message.isImage -> {
                        AsyncImage(
                            model = ImageUtils.base64ToBitmap(message.content),
                            contentDescription = null,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    message.isVoice -> {
                        AudioPlayer(message.content, onPlayVoice, onStopVoicePlayback)
                    }
                    message.isVideo -> {
                        VideoPlayer(Uri.parse(message.content))
                    }
                    else -> {
                        Text(message.content, color = contentColor)
                    }
                }

                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                    )
                    if (message.isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = when(message.status) {
                                MessageStatus.SENT -> Icons.Default.Check
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                MessageStatus.READ -> Icons.Default.DoneAll
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (message.status == MessageStatus.READ) Color.Cyan else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaStagingArea(
    media: List<ChatViewModel.StagedMedia>,
    onRemove: (Uri) -> Unit,
    cornerRadius: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(media) { item ->
                Box {
                    Surface(
                        shape = RoundedCornerShape(cornerRadius.dp),
                        modifier = Modifier.size(100.dp),
                        tonalElevation = 2.dp
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(
                        onClick = { onRemove(item.uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyPreview(reply: ChatViewModel.ReplyInfo, onClear: () -> Unit, cornerRadius: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(cornerRadius.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reply.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(reply.messageContent, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, null)
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
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    recordingDuration: Long,
    cornerRadius: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.Add, "Attach")
            }

            if (recordingDuration > 0) {
                Text(
                    String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            } else {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ghost Message...") },
                    shape = RoundedCornerShape(cornerRadius.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            if (text.isNotBlank()) {
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                var isRecording by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {},
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isRecording = true
                                onStartVoice()
                                tryAwaitRelease()
                                isRecording = false
                                onStopVoice()
                            }
                        )
                    }
                ) {
                    Icon(
                        if (isRecording) Icons.Default.MicNone else Icons.Default.Mic,
                        "Voice",
                        tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = if (isRecording) Modifier.graphicsLayer { scaleX = 1.2f; scaleY = 1.2f } else Modifier
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onFile: () -> Unit,
    onDismiss: () -> Unit,
    cornerRadius: Int
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = cornerRadius.dp, topEnd = cornerRadius.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("ATTACHMENTS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            AttachmentOption("Photos & Videos", Icons.Default.Image, onPhoto)
            AttachmentOption("Camera", Icons.Default.CameraAlt, onPhoto)
            AttachmentOption("File", Icons.Default.InsertDriveFile, onFile)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AttachmentOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}
