package com.kai.ghostmesh.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AudioPlayer(
    base64: String,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (isPlaying) {
                onStop()
                isPlaying = false
            } else {
                onPlay(base64)
                isPlaying = true
            }
        }) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        Text("Voice Note", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.width(8.dp))

        // Simple Linear Progress could be added if duration was known
        LinearProgressIndicator(
            progress = { 0f },
            modifier = Modifier.weight(1f)
        )
    }
}
