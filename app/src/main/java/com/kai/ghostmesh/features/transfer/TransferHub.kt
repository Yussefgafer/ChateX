package com.kai.ghostmesh.features.transfer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.mesh.FileTransferManager
import com.kai.ghostmesh.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferHubScreen(
    transfers: List<FileTransferManager.FileTransfer>,
    onCancel: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    var interactingIndex by remember { mutableStateOf(-1) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MediumTopAppBar(
                    title = { Text("TRANSFERS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium, letterSpacing = 2.sp) },
                    navigationIcon = {
                        ExpressiveIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (transfers.isEmpty()) {
                    EmptyTransferState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(transfers, key = { _, t -> t.fileId }) { index, transfer ->
                            val isNeighborInteracting = interactingIndex != -1 && interactingIndex != index
                            TransferItem(
                                transfer = transfer,
                                isInteracting = interactingIndex == index,
                                modifier = Modifier.proximityDisplacement(isNeighborInteracted = isNeighborInteracting),
                                onInteracting = { interactingIndex = if (it) index else -1 },
                                onCancel = onCancel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTransferState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(72.dp).alpha(0.1f), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("DATA STREAM IDLE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun TransferItem(
    transfer: FileTransferManager.FileTransfer,
    isInteracting: Boolean,
    modifier: Modifier = Modifier,
    onInteracting: (Boolean) -> Unit = {},
    onCancel: (String) -> Unit
) {
    val progress = transfer.bytesTransferred.toFloat() / transfer.totalSize.coerceAtLeast(1)

    val dynamicRadius by animateDpAsState(
        targetValue = if (isInteracting) 12.dp else 24.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
        label = "radius"
    )

    ExpressiveCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onInteracting(!isInteracting) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.fileName, maxLines = 1, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(transfer.senderId.take(12).uppercase(), style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.alpha(0.6f))
                }
            }

            ExpressiveIconButton(onClick = { onCancel(transfer.fileId) }, containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.error) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
