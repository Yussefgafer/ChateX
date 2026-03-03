package com.kai.ghostmesh.features.transfer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.mesh.FileTransferManager
import com.kai.ghostmesh.core.ui.components.ExpressiveIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferHubScreen(
    transfers: List<FileTransferManager.FileTransfer>,
    onCancel: (String) -> Unit,
    onBack: () -> Unit
) {
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No active spectral flows", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(transfers) { transfer ->
                    TransferItem(transfer, onCancel)
                }
            }
        }
    }
}

@Composable
fun TransferItem(transfer: FileTransferManager.FileTransfer, onCancel: (String) -> Unit) {
    val progress = transfer.bytesTransferred.toFloat() / transfer.totalSize.coerceAtLeast(1)

    ListItem(
        headlineContent = { Text(transfer.fileName, maxLines = 1) },
        supportingContent = {
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("${(progress * 100).toInt()}% • ${transfer.senderId.take(8)}", style = MaterialTheme.typography.labelSmall)
            }
        },
        leadingContent = {
            Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            ExpressiveIconButton(onClick = { onCancel(transfer.fileId) }) {
                Icon(Icons.Default.Close, null)
            }
        }
    )
}
