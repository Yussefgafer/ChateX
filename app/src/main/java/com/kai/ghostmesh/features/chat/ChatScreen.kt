package com.kai.ghostmesh.features.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.features.discovery.TransportIcon
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(
    ghostId: String,
    ghostName: String,
    messages: List<Message>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onSendImage: (android.net.Uri) -> Unit,
    onSendVideo: (android.net.Uri) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    replyToMessage: ChatViewModel.ReplyInfo? = null,
    onSetReply: ((String, String, String) -> Unit)? = null,
    onClearReply: (() -> Unit)? = null,
    cornerRadius: Int = 16,
    transportType: String? = null
) {
    var textState by remember { mutableStateOf("") }
    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onSendImage(it) } }
    
    val videoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onSendVideo(it) } }

    var isRecording by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(textState) { onTypingChange(textState.isNotBlank()) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.semantics { contentDescription = "Chat with $ghostName" }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ghostName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            TransportIcon(transportType)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp).semantics { contentDescription = "E2E Encrypted" }, tint = MaterialTheme.colorScheme.primary)
                        }
                        AnimatedVisibility(visible = isTyping) {
                            Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    ExpressiveIconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Go Back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    ExpressiveIconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = "Message List" },
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val reversedMessages = messages.asReversed()
                itemsIndexed(reversedMessages, key = { _, msg -> msg.id }) { index, msg ->
                    val prevMsg = reversedMessages.getOrNull(index + 1)
                    val nextMsg = reversedMessages.getOrNull(index - 1)
                    
                    val isFirstInGroup = prevMsg?.sender != msg.sender
                    val isLastInGroup = nextMsg?.sender != msg.sender

                    SwipeableMessageItem(
                        msg = msg,
                        isFirstInGroup = isFirstInGroup,
                        isLastInGroup = isLastInGroup,
                        onPlayVoice = onPlayVoice,
                        onDelete = onDeleteMessage,
                        onReply = { onSetReply?.invoke(msg.id, msg.content, msg.sender) },
                        cornerRadius = cornerRadius
                    )
                }
            }

            ChatInput(
                text = textState,
                onTextChange = { textState = it },
                onSend = { onSendMessage(it); textState = "" },
                isRecording = isRecording,
                onRecordingChange = { isRecording = it },
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onActionClick = { showActionMenu = true },
                replyTo = replyToMessage,
                onClearReply = { onClearReply?.invoke() }
            )
        }
    }

    if (showActionMenu) {
        ModalBottomSheet(onDismissRequest = { showActionMenu = false }) {
            ActionMenuContent(
                onImageClick = { imageLauncher.launch("image/*"); showActionMenu = false },
                onVideoClick = { videoLauncher.launch("video/*"); showActionMenu = false },
                onFileClick = { /* File picker */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    isRecording: Boolean,
    onRecordingChange: (Boolean) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onActionClick: () -> Unit,
    replyTo: ChatViewModel.ReplyInfo? = null,
    onClearReply: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column {
            if (replyTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(replyTo.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(replyTo.messageContent, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    ExpressiveIconButton(onClick = onClearReply) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                }
            }

            Row(
                modifier = Modifier.padding(8.dp).navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveIconButton(onClick = onActionClick) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
                
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f).animateContentSize(),
                    placeholder = { Text("Spectral transmission...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                AnimatedContent(targetState = text.isNotBlank(), label = "send_button") { isText ->
                    if (isText) {
                        ExpressiveIconButton(
                            onClick = { onSend(text) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        ExpressiveIconButton(
                            modifier = Modifier
                                .semantics { contentDescription = if (isRecording) "Recording Voice" else "Record Voice" }
                                .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            onRecordingChange(true)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onStartVoice()
                                            awaitRelease()
                                        } finally {
                                            onRecordingChange(false)
                                            onStopVoice()
                                        }
                                    }
                                )
                            },
                            onClick = {}
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.MicNone else Icons.Default.Mic, 
                                null, 
                                tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeableMessageItem(
    msg: Message,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onPlayVoice: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReply: () -> Unit,
    cornerRadius: Int = 16
) {
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateIntAsState(targetValue = offsetX.roundToInt())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                state = rememberDraggableState { delta ->
                    if (offsetX + delta > 0) offsetX += delta * 0.5f
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (offsetX > 150f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReply()
                    }
                    offsetX = 0f
                }
            )
            .semantics { contentDescription = "Swipe to reply to message from ${msg.sender}" }
    ) {
        if (offsetX > 20f) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = (offsetX / 150f).coerceIn(0f, 1f))
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset, 0) }
                .fillMaxWidth()
        ) {
            MessageBubble(
                msg = msg,
                isFirstInGroup = isFirstInGroup,
                isLastInGroup = isLastInGroup,
                onPlayVoice = onPlayVoice,
                onDelete = onDelete,
                onReply = onReply,
                cornerRadius = cornerRadius
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageBubble(
    msg: Message,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onPlayVoice: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReply: () -> Unit,
    cornerRadius: Int = 16
) {
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (msg.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    val baseRadius = cornerRadius.dp
    val smallRadius = (cornerRadius / 4).dp
    val ts by animateDpAsState(targetValue = if (msg.isMe || isFirstInGroup) baseRadius else smallRadius, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
    val te by animateDpAsState(targetValue = if (!msg.isMe || isFirstInGroup) baseRadius else smallRadius, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
    val bs by animateDpAsState(targetValue = if (msg.isMe || isLastInGroup) baseRadius else smallRadius, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
    val be by animateDpAsState(targetValue = if (!msg.isMe || isLastInGroup) baseRadius else smallRadius, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
    val shape = RoundedCornerShape(topStart = ts, topEnd = te, bottomStart = bs, bottomEnd = be)

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (isFirstInGroup) 4.dp else 1.dp),
        horizontalAlignment = alignment
    ) {
        if (isFirstInGroup && !msg.isMe) {
            Text(
                msg.sender,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = baseRadius, bottom = 2.dp)
            )
        }
        Surface(
            color = bubbleColor,
            contentColor = contentColor,
            shape = shape,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .physicalTilt()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { /* Show Menu */ }
                )
                .semantics { contentDescription = "Message from ${msg.sender}: ${msg.content}" }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (msg.replyToContent != null) {
                    ReplyHeader(msg.replyToSender, msg.replyToContent)
                    Spacer(Modifier.height(8.dp))
                }
                
                when {
                    msg.isImage -> MessageImage(msg.content)
                    msg.isVoice -> MessageVoice(msg.content, onPlayVoice)
                    msg.isVideo -> MessageVideo(msg.content)
                    else -> Text(msg.content, style = MaterialTheme.typography.bodyLarge)
                }
                
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                    if (msg.isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (msg.status == MessageStatus.READ) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (msg.status == MessageStatus.READ) "Read" else "Sent",
                            modifier = Modifier.size(12.dp),
                            tint = if (msg.status == MessageStatus.READ) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyHeader(sender: String?, content: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(2.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(sender ?: "Unknown", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(content, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
fun ActionMenuContent(onImageClick: () -> Unit, onVideoClick: () -> Unit, onFileClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        ListItem(
            headlineContent = { Text("Photo Gallery") },
            leadingContent = { Icon(Icons.Default.Photo, null) },
            modifier = Modifier.clickable { onImageClick() }.semantics { contentDescription = "Pick Image from Gallery" }
        )
        ListItem(
            headlineContent = { Text("Video Gallery") },
            leadingContent = { Icon(Icons.Default.VideoFile, null) },
            modifier = Modifier.clickable { onVideoClick() }.semantics { contentDescription = "Pick Video from Gallery" }
        )
        ListItem(
            headlineContent = { Text("File") },
            leadingContent = { Icon(Icons.Default.AttachFile, null) },
            modifier = Modifier.clickable { onFileClick() }.semantics { contentDescription = "Attach File from Storage" }
        )
    }
}

@Composable
fun MessageImage(content: String) {
    val bitmap = remember(content) {
        try {
            val bytes = Base64.decode(content, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Image message",
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun MessageVideo(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
        Text("Spectral Video", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}

@Composable
fun MessageVoice(content: String, onPlay: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onPlay(content) }.semantics { contentDescription = "Voice Message, Play" }) {
        Icon(Icons.Default.PlayArrow, null)
        Spacer(Modifier.width(8.dp))
        Text("Voice Message", style = MaterialTheme.typography.bodyMedium)
    }
}
