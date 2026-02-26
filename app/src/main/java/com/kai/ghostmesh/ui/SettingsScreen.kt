package com.kai.ghostmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border // 🚀 Missing Import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    isDiscoveryEnabled: Boolean,
    isAdvertisingEnabled: Boolean,
    isStealthMode: Boolean,
    isHapticEnabled: Boolean,
    isEncryptionEnabled: Boolean,
    selfDestructSeconds: Int,
    hopLimit: Int,
    onProfileChange: (String, String, Int?) -> Unit,
    onToggleDiscovery: (Boolean) -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onToggleStealth: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEncryption: (Boolean) -> Unit,
    onSetSelfDestruct: (Int) -> Unit,
    onSetHopLimit: (Int) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit
) {
    var nameState by remember { mutableStateOf(profile.name) }
    var statusState by remember { mutableStateOf(profile.status) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChateX Console") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            
            SettingsHeader("Spectral Presence")
            OutlinedTextField(value = nameState, onValueChange = { nameState = it; onProfileChange(it, statusState, null) }, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = statusState, onValueChange = { statusState = it; onProfileChange(nameState, it, null) }, label = { Text("Void Status") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Soul Hue (UI Theme)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val colors = listOf(0xFF00FF7F, 0xFFFF3131, 0xFFBB86FC, 0xFF00BFFF, 0xFFFFD700, 0xFFFF69B4)
                colors.forEach { colorHex ->
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(colorHex)).border(if (profile.color == colorHex.toInt()) 2.dp else 0.dp, Color.White, CircleShape).clickable { onProfileChange(nameState, statusState, colorHex.toInt()) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsHeader("Mesh Engine")
            SettingsListItem(title = "Stealth Mode (Spectre)", icon = Icons.Default.VisibilityOff, subtitle = "Receive only, stay hidden", checked = isStealthMode, onCheckedChange = onToggleStealth)
            SettingsListItem(title = "Auto Discovery", icon = Icons.Default.Radar, checked = isDiscoveryEnabled, onCheckedChange = onToggleDiscovery)
            SettingsListItem(title = "Mesh Relay", icon = Icons.Default.Route, subtitle = "Enable multi-hop routing", checked = isAdvertisingEnabled, onCheckedChange = onToggleAdvertising)
            
            ListItem(
                headlineContent = { Text("Max Hop Count: $hopLimit") },
                supportingContent = { Slider(value = hopLimit.toFloat(), onValueChange = { onSetHopLimit(it.toInt()) }, valueRange = 1f..10f, steps = 4) },
                leadingContent = { Icon(Icons.Default.Speed, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsHeader("Void Security")
            SettingsListItem(title = "E2EE Encryption", icon = Icons.Default.Security, checked = isEncryptionEnabled, onCheckedChange = onToggleEncryption)
            
            ListItem(
                headlineContent = { Text("Self-Destruct Messages") },
                supportingContent = {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 10, 30, 60).forEach { sec -> FilterChip(selected = selfDestructSeconds == sec, onClick = { onSetSelfDestruct(sec) }, label = { Text(if (sec == 0) "Off" else "${sec}s") }) }
                    }
                },
                leadingContent = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.Red) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsHeader("Void Maintenance")
            SettingsListItem(title = "Tactile Mesh (Haptics)", icon = Icons.Default.TouchApp, checked = isHapticEnabled, onCheckedChange = onToggleHaptic)
            
            Button(onClick = onClearChat, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Purge Spectral Archives")
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsHeader(text: String) {
    Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
}

@Composable
fun SettingsListItem(title: String, icon: ImageVector, subtitle: String? = null, checked: Boolean? = null, onCheckedChange: ((Boolean) -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        leadingContent = { Icon(icon, null) },
        trailingContent = when {
            checked != null -> { { Switch(checked = checked, onCheckedChange = onCheckedChange!!) } }
            trailing != null -> trailing
            else -> null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
