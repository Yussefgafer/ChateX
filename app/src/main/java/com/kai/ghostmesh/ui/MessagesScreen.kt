package com.kai.ghostmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.data.local.ProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    profiles: List<ProfileEntity>,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Spectral Archives", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            if (profiles.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("The archives are empty...", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(profiles) { profile ->
                        MessageItem(profile) { onNavigateToChat(profile.id, profile.name) }
                    }
                }
            }
        }

        // Bottom Nav
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = MaterialTheme.shapes.extraLarge,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToRadar) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MessageItem(profile: ProfileEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(profile.color).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(profile.name.take(1).uppercase(), color = Color(profile.color), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(profile.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(profile.status, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
        }
    }
}
