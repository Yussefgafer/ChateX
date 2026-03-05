import os

def write_expressive():
    content = """package com.kai.ghostmesh.core.ui.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import com.kai.ghostmesh.core.ui.theme.GhostMotion

const val DURATION_PER_SHAPE_MS = 650

object MaterialShapes {
    fun softBurst() = RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.7f, rounding = CornerRounding(0.3f))
    fun cookie9() = RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.85f, rounding = CornerRounding(0.15f))
    fun pentagon() = RoundedPolygon.circle(numVertices = 5)
    fun pill() = RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.5f))
    fun sunny() = RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.5f, rounding = CornerRounding(0.3f))
    fun cookie4() = RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.8f, rounding = CornerRounding(0.2f))
    fun oval() = RoundedPolygon.rectangle(width = 1.4f, height = 2f, rounding = CornerRounding(1f))
    fun diamond() = RoundedPolygon.circle(numVertices = 4)
    fun leaf() = RoundedPolygon.star(numVerticesPerRadius = 2, innerRadius = 0.4f, rounding = CornerRounding(0.8f))
    fun hexagon() = RoundedPolygon.circle(numVertices = 6)

    val IndeterminateSequence = listOf(
        softBurst(), cookie9(), pentagon(), pill(), sunny(), cookie4(), oval(), diamond(), leaf(), hexagon()
    )
    val LoadingSequence = listOf(
        softBurst(), cookie9(), pentagon(), pill(), sunny(), cookie4(), oval()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoercedExpressiveCard(
    userRadius: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = GhostMotion.TactileSpring,
        label = "card_morph"
    )

    val shapeStart = remember(userRadius) { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(userRadius.coerceIn(0f, 100f) / 100f)) }
    val shapeEnd = remember(userRadius) { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding((userRadius * 1.2f).coerceIn(0f, 100f) / 100f)) }
    val morph = remember(userRadius) { Morph(shapeStart, shapeEnd) }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        interactionSource = interactionSource,
        color = Color.Transparent,
        modifier = modifier
            .physicalTilt()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(16.dp)
                } else Modifier
            )
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val path = Path()
                    // If toPath expects android.graphics.Path, we use asAndroidPath()
                    // If it expects Compose Path, we use path
                    // Based on previous error, it expects androidx.compose.ui.graphics.Path
                    morph.toPath(morphProgress, path)

                    matrix.reset()
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.translate(1f, 1f)
                    path.transform(matrix)

                    drawPath(path, color = containerColor)
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shapeProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = GhostMotion.TactileSpring,
        label = "btn_morph"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .jellyClickable(onClick = onClick, enabled = enabled)
            .drawWithCache {
                val matrix = Matrix()
                val s1 = MaterialShapes.pill()
                val s2 = RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.2f))
                val morph = Morph(s1, s2)

                onDrawBehind {
                    val path = Path()
                    morph.toPath(shapeProgress, path)

                    matrix.reset()
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.translate(1f, 1f)
                    path.transform(matrix)

                    drawPath(path, color = if (enabled) containerColor else containerColor.copy(alpha = 0.3f))
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingDiscoveryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_morph")
    val shapes = MaterialShapes.IndeterminateSequence

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = shapes.size * DURATION_PER_SHAPE_MS
                shapes.indices.forEach { i ->
                    i.toFloat() at (i * DURATION_PER_SHAPE_MS) using GhostMotion.EmphasizedEasing
                }
                shapes.size.toFloat() at (shapes.size * DURATION_PER_SHAPE_MS)
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "morph_factor"
    )

    val linearFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(shapes.size * DURATION_PER_SHAPE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "linear_factor"
    )

    val index = morphFactor.toInt().coerceIn(0, shapes.size - 1)
    val nextIndex = (index + 1) % shapes.size
    val localEasedProgress = (morphFactor - index.toFloat()).coerceIn(0f, 1f)
    val localLinearProgress = (linearFactor - index.toFloat()).coerceIn(0f, 1f)

    val currentShape = shapes[index]
    val nextShape = shapes[nextIndex]
    val morph = remember(index, nextIndex) { Morph(currentShape, nextShape) }
    val animatedProgress by animateFloatAsState(targetValue = localEasedProgress, animationSpec = GhostMotion.MorphSpring)

    val rotation = (140f * index) + (50f * localLinearProgress) + (90f * localEasedProgress)

    Box(
        modifier = modifier
            .size(72.dp)
            .jellyClickable(onClick = onClick)
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val path = Path()
                    morph.toPath(animatedProgress, path)

                    matrix.reset()
                    matrix.rotateZ(rotation)
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.translate(1f, 1f)
                    path.transform(matrix)

                    drawPath(path, color = containerColor)
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Radar, "Discovery", tint = contentColor, modifier = Modifier.size(32.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MD3ELoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_morph")
    val shapes = MaterialShapes.LoadingSequence

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = shapes.size * DURATION_PER_SHAPE_MS
                shapes.indices.forEach { i ->
                    i.toFloat() at (i * DURATION_PER_SHAPE_MS) using GhostMotion.EmphasizedEasing
                }
                shapes.size.toFloat() at (shapes.size * DURATION_PER_SHAPE_MS)
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_morph"
    )

    val linearFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(shapes.size * DURATION_PER_SHAPE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_linear"
    )

    val index = morphFactor.toInt().coerceIn(0, shapes.size - 1)
    val nextIndex = (index + 1) % shapes.size
    val localEasedProgress = (morphFactor - index.toFloat()).coerceIn(0f, 1f)
    val localLinearProgress = (linearFactor - index.toFloat()).coerceIn(0f, 1f)

    val currentShape = shapes[index]
    val nextShape = shapes[nextIndex]
    val morph = remember(index, nextIndex) { Morph(currentShape, nextShape) }
    val animatedProgress by animateFloatAsState(targetValue = localEasedProgress, animationSpec = GhostMotion.MorphSpring)

    val rotation = (140f * index) + (50f * localLinearProgress) + (90f * localEasedProgress)

    Box(
        modifier = modifier
            .size(48.dp)
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val path = Path()
                    morph.toPath(animatedProgress, path)

                    matrix.reset()
                    matrix.rotateZ(rotation)
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.translate(1f, 1f)
                    path.transform(matrix)

                    drawPath(path, color = color)
                }
            }
    )
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .jellyClickable(onClick = onClick, enabled = enabled)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
    }
}

@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.physicalTilt(),
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

fun RoundedPolygon.Companion.pill(): RoundedPolygon {
    return RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.5f))
}
"""
    with open('app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt', 'w') as f:
        f.write(content)

def write_chat():
    content = """package com.kai.ghostmesh.features.chat

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
import androidx.compose.ui.draw.alpha
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
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

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
                    reverseLayout = true,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message, onDeleteMessage, onPlayVoice, onSetReply, cornerRadius)
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
            onPhoto = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
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
                        color = Color.Black.copy(alpha = 0.1f),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onPlayVoice(message.content) }) {
                                Icon(Icons.Default.PlayArrow, null)
                            }
                            Text("Voice Note", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    else -> {
                        Text(message.content, color = contentColor)
                    }
                }

                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.alpha(0.6f)
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
                            tint = if (message.status == MessageStatus.READ) Color.Cyan else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaStagingArea(media: List<ChatViewModel.StagedMedia>, onRemove: (Uri) -> Unit, cornerRadius: Int) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(media) { item ->
            Box {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(cornerRadius.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemove(item.uri) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
"""
    with open('app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt', 'w') as f:
        f.write(content)

def write_main():
    content = """package com.kai.ghostmesh

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.features.chat.ChatScreen
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.features.discovery.DiscoveryScreen
import com.kai.ghostmesh.features.discovery.DiscoveryViewModel
import com.kai.ghostmesh.features.messages.MessagesScreen
import com.kai.ghostmesh.features.messages.MessagesViewModel
import com.kai.ghostmesh.features.settings.SettingsScreen
import com.kai.ghostmesh.features.settings.SettingsViewModel
import com.kai.ghostmesh.service.MeshService
import com.kai.ghostmesh.ui.theme.GhostMeshTheme

class MainActivity : ComponentActivity() {

    private var meshService: MeshService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeshService.MeshBinder
            meshService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            meshService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serviceIntent = Intent(this, MeshService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            GhostMeshTheme(darkTheme = when(themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }) {
                val navController = rememberNavController()
                val chatViewModel: ChatViewModel = viewModel()
                val discoveryViewModel: DiscoveryViewModel = viewModel()
                val messagesViewModel: MessagesViewModel = viewModel()

                val isServiceReady by meshService?.isReady?.collectAsState() ?: remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isServiceReady) {
                        NavHost(navController = navController, startDestination = "messages") {
                            composable("messages") {
                                val chats by messagesViewModel.recentChats.collectAsState(emptyList())
                                MessagesScreen(
                                    chats = chats,
                                    onChatClick = { peerId, peerName ->
                                        chatViewModel.setActiveChat(peerId)
                                        navController.navigate("chat/$peerId/$peerName")
                                    },
                                    onDiscoveryClick = { navController.navigate("discovery") },
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            }
                            composable("discovery") {
                                val connectedNodes by discoveryViewModel.connectedNodes.collectAsState()
                                val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                DiscoveryScreen(
                                    connectedNodes = connectedNodes,
                                    cornerRadius = cornerRadius,
                                    onNodeClick = { peerId, peerName ->
                                        chatViewModel.setActiveChat(peerId)
                                        navController.navigate("chat/$peerId/$peerName")
                                    },
                                    onShout = { discoveryViewModel.shout(it) }
                                )
                            }
                            composable("settings") {
                                val profile by settingsViewModel.userProfile.collectAsState()
                                val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val fontScale by settingsViewModel.fontScale.collectAsState()
                                val isNearbyEnabled by settingsViewModel.isNearbyEnabled.collectAsState()
                                val isBluetoothEnabled by settingsViewModel.isBluetoothEnabled.collectAsState()
                                val isLanEnabled by settingsViewModel.isLanEnabled.collectAsState()
                                val isWifiDirectEnabled by settingsViewModel.isWifiDirectEnabled.collectAsState()
                                val isStealthMode by settingsViewModel.isStealthMode.collectAsState()
                                val isEncryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
                                val autoDownloadImages by settingsViewModel.autoDownloadImages.collectAsState()
                                val autoDownloadVideos by settingsViewModel.autoDownloadVideos.collectAsState()
                                val autoDownloadFiles by settingsViewModel.autoDownloadFiles.collectAsState()
                                val downloadSizeLimit by settingsViewModel.downloadSizeLimit.collectAsState()
                                val mnemonic by settingsViewModel.mnemonic.collectAsState()

                                SettingsScreen(
                                    profile = profile,
                                    isStealthMode = isStealthMode,
                                    isEncryptionEnabled = isEncryptionEnabled,
                                    autoDownloadImages = autoDownloadImages,
                                    autoDownloadVideos = autoDownloadVideos,
                                    autoDownloadFiles = autoDownloadFiles,
                                    downloadSizeLimit = downloadSizeLimit,
                                    mnemonic = mnemonic,
                                    themeMode = themeMode,
                                    cornerRadius = cornerRadius,
                                    fontScale = fontScale,
                                    isNearbyEnabled = isNearbyEnabled,
                                    isBluetoothEnabled = isBluetoothEnabled,
                                    isLanEnabled = isLanEnabled,
                                    isWifiDirectEnabled = isWifiDirectEnabled,
                                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                                    onToggleAutoDownloadImages = { settingsViewModel.updateSetting("auto_download_images", it) },
                                    onToggleAutoDownloadVideos = { settingsViewModel.updateSetting("auto_download_videos", it) },
                                    onToggleAutoDownloadFiles = { settingsViewModel.updateSetting("auto_download_files", it) },
                                    onSetDownloadSizeLimit = { settingsViewModel.updateSetting("download_size_limit", it) },
                                    onGenerateBackup = { settingsViewModel.generateBackupMnemonic() },
                                    onClearChat = { settingsViewModel.clearHistory() },
                                    onSetTheme = { settingsViewModel.updateSetting("theme_mode", it) },
                                    onSetCornerRadius = { settingsViewModel.updateSetting("corner_radius", it) },
                                    onSetFontScale = { settingsViewModel.updateSetting("font_scale", it) },
                                    onToggleNearby = { settingsViewModel.updateSetting("enable_nearby", it) },
                                    onToggleBluetooth = { settingsViewModel.updateSetting("enable_bluetooth", it) },
                                    onToggleLan = { settingsViewModel.updateSetting("enable_lan", it) },
                                    onToggleWifiDirect = { settingsViewModel.updateSetting("enable_wifi_direct", it) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("chat/{peerId}/{peerName}", arguments = listOf(
                                navArgument("peerId") { type = NavType.StringType },
                                navArgument("peerName") { type = NavType.StringType }
                            )) { backStackEntry ->
                                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                                val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"

                                val chatMessages by chatViewModel.messages.collectAsState()
                                val stagedMedia by chatViewModel.stagedMedia.collectAsState()
                                val recordingDuration by chatViewModel.recordingDuration.collectAsState()
                                val currentCornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val encryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
                                val currentSelfDestruct by settingsViewModel.selfDestructSeconds.collectAsState()
                                val currentHopLimit by settingsViewModel.hopLimit.collectAsState()
                                val userProfile by settingsViewModel.userProfile.collectAsState()
                                val typingPeers by chatViewModel.typingPeers.collectAsState()
                                val replyToMessage by chatViewModel.replyToMessage.collectAsState()

                                ChatScreen(
                                    peerId = peerId, peerName = peerName,
                                    messages = chatMessages,
                                    isTyping = typingPeers.contains(peerId),
                                    onSendMessage = { chatViewModel.sendMessage(it, encryptionEnabled, currentSelfDestruct, currentHopLimit, userProfile) },
                                    onSendImage = { chatViewModel.stageMedia(it, ChatViewModel.MediaType.IMAGE) },
                                    onSendVideo = { chatViewModel.stageMedia(it, ChatViewModel.MediaType.VIDEO) },
                                    onStartVoice = { chatViewModel.startRecording() },
                                    onStopVoice = { chatViewModel.stopRecording() },
                                    onPlayVoice = { chatViewModel.playVoice(it) },
                                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                                    onTypingChange = { chatViewModel.sendTyping(it, userProfile) },
                                    onBack = { navController.popBackStack() },
                                    replyToMessage = replyToMessage,
                                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                                    onClearReply = { chatViewModel.clearReply() },
                                    stagedMedia = stagedMedia,
                                    onStageMedia = { uri, type -> chatViewModel.stageMedia(uri, type) },
                                    onUnstageMedia = { chatViewModel.unstageMedia(it) },
                                    recordingDuration = recordingDuration,
                                    cornerRadius = currentCornerRadius,
                                    transportType = ""
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
"""
    with open('app/src/main/java/com/kai/ghostmesh/MainActivity.kt', 'w') as f:
        f.write(content)

write_expressive()
write_chat()
write_main()
