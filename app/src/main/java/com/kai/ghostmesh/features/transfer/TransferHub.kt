package com.kai.ghostmesh.features.transfer

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.mesh.FileTransferManager
import com.kai.ghostmesh.core.ui.components.ExpressiveIconButton
import com.kai.ghostmesh.core.ui.components.GhostShaders
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferHubScreen(
    transfers: List<FileTransferManager.FileTransfer>,
    onCancel: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()

    // Dynamic Corner Radius based on scroll velocity for "Physical" feel
    val velocity = remember { derivedStateOf { abs(listState.firstVisibleItemScrollOffset.toFloat() % 100) } }
    val dynamicCornerRadius by animateDpAsState(
        targetValue = (28 + (velocity.value / 10)).dp.coerceAtMost(48.dp),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamic_corner"
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Transfer Hub", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    ExpressiveIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (transfers.isEmpty()) {
            EmptyTransferState(padding)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transfers, key = { it.fileId }) { transfer ->
                    TransferItem(transfer, dynamicCornerRadius, onCancel)
                }
            }
        }
    }
}

@Composable
fun EmptyTransferState(padding: PaddingValues) {
    val time = rememberInfiniteTransition(label = "noise").animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "time"
    )

    val noiseBrush = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GhostShaders.createNoiseBrush(time.value, 1000f, 1000f) ?: SolidColor(Color.Transparent)
    } else {
        SolidColor(Color.Transparent)
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .background(noiseBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp).alpha(0.1f),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No active transfers",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TransferItem(
    transfer: FileTransferManager.FileTransfer,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onCancel: (String) -> Unit
) {
    val progress = transfer.bytesTransferred.toFloat() / transfer.totalSize.coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(transfer.fileName, maxLines = 1, fontWeight = FontWeight.Bold) },
            supportingContent = {
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "${(progress * 100).toInt()}% • ${transfer.senderId.take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            leadingContent = {
                Icon(
                    Icons.Default.CloudUpload,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                ExpressiveIconButton(onClick = { onCancel(transfer.fileId) }) {
                    Icon(Icons.Default.Close, null)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
