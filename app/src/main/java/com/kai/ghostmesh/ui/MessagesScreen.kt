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
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Spectral Archives", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors( // 🚀 Fixed Deprecation
                    containerColor = MaterialTheme.colorScheme.background, 
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), tonalElevation = 0.dp) {
                NavigationBarItem(selected = false, onClick = onNavigateToRadar, icon = { Icon(Icons.Default.Radar, null) }, label = { Text("Radar") })
                NavigationBarItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.ChatBubble, null) }, label = { Text("Archives") })
                NavigationBarItem(selected = false, onClick = onNavigateToSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("The void is empty...", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(profiles) { profile ->
                        MessageItem(profile) { onNavigateToChat(profile.id, profile.name) }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 72.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(profile: ProfileEntity, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(profile.status, color = Color.Gray, maxLines = 1) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(profile.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(profile.name.take(1).uppercase(), color = Color(profile.color), style = MaterialTheme.typography.titleMedium)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
