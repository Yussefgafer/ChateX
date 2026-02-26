package com.kai.ghostmesh.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.model.UserProfile
import com.kai.ghostmesh.ui.components.HapticIconButton
import com.kai.ghostmesh.ui.components.HapticButton

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
    packetsSent: Int,
    packetsReceived: Int,
    animationSpeed: Float,
    hapticIntensity: Int,
    messagePreview: Boolean,
    autoReadReceipts: Boolean,
    compactMode: Boolean,
    showTimestamps: Boolean,
    connectionTimeout: Int,
    maxImageSize: Int,
    themeMode: Int,
    onProfileChange: (String, String, Int?) -> Unit,
    onToggleDiscovery: (Boolean) -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onToggleStealth: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEncryption: (Boolean) -> Unit,
    onSetSelfDestruct: (Int) -> Unit,
    onSetHopLimit: (Int) -> Unit,
    onSetAnimationSpeed: (Float) -> Unit,
    onSetHapticIntensity: (Int) -> Unit,
    onToggleMessagePreview: (Boolean) -> Unit,
    onToggleAutoReadReceipts: (Boolean) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onToggleShowTimestamps: (Boolean) -> Unit,
    onSetConnectionTimeout: (Int) -> Unit,
    onSetMaxImageSize: (Int) -> Unit,
    onSetThemeMode: (Int) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit
) {
    var nameState by remember { mutableStateOf(profile.name) }
    var statusState by remember { mutableStateOf(profile.status) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SPECTRAL CONSOLE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HapticIconButton(onClick = onBack) { Icon(Icons.Default.Radar, "Radar", tint = Color.Gray) }
                        HapticIconButton(onClick = onBack) { Icon(Icons.Default.ChatBubble, "Archives", tint = Color.Gray) }
                        HapticIconButton(onClick = { }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) { 
                            Icon(Icons.Default.Settings, "Console", tint = Color.Black) 
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            
            ConsoleSection("IDENTITY") {
                OutlinedTextField(
                    value = nameState, 
                    onValueChange = { nameState = it; onProfileChange(it, statusState, null) }, 
                    label = { Text("NICKNAME") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = statusState, 
                    onValueChange = { statusState = it; onProfileChange(nameState, it, null) }, 
                    label = { Text("STATUS") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ConsoleSection("TELEMETRY") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TelemetryItem("SENT", "$packetsSent")
                    TelemetryItem("RECV", "$packetsReceived")
                    TelemetryItem("LINK", "STABLE")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ConsoleSection("MESH ENGINE") {
                ConsoleSwitch("STEALTH MODE", isStealthMode, onToggleStealth)
                ConsoleSwitch("AUTO DISCOVERY", isDiscoveryEnabled, onToggleDiscovery)
                ConsoleSwitch("MESH RELAY", isAdvertisingEnabled, onToggleAdvertising)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("HOP LIMIT: $hopLimit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = hopLimit.toFloat(), 
                    onValueChange = { onSetHopLimit(it.toInt()) }, 
                    valueRange = 1f..10f, 
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ConsoleSection("DISPLAY") {
                ConsoleSwitch("MESSAGE PREVIEW", messagePreview, onToggleMessagePreview)
                ConsoleSwitch("AUTO READ RECEIPTS", autoReadReceipts, onToggleAutoReadReceipts)
                ConsoleSwitch("COMPACT MODE", compactMode, onToggleCompactMode)
                ConsoleSwitch("SHOW TIMESTAMPS", showTimestamps, onToggleShowTimestamps)

                Spacer(modifier = Modifier.height(8.dp))
                Text("ANIMATION SPEED: ${String.format(java.util.Locale.getDefault(), "%.1fx", animationSpeed)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = animationSpeed,
                    onValueChange = { onSetAnimationSpeed(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(8.dp))
                val hapticLabels = listOf("OFF", "LIGHT", "MEDIUM", "STRONG")
                Text("HAPTIC INTENSITY: ${hapticLabels[hapticIntensity]}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = hapticIntensity.toFloat(),
                    onValueChange = { onSetHapticIntensity(it.toInt()) },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                val themeOptions = listOf("SYSTEM", "DARK", "LIGHT", "SPECTRAL")
                var themeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = themeOptions[themeMode],
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    ExposedDropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        themeOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { onSetThemeMode(index); themeExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ConsoleSection("NETWORK") {
                Spacer(modifier = Modifier.height(8.dp))
                Text("MAX HOPS: $hopLimit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = hopLimit.toFloat(), 
                    onValueChange = { onSetHopLimit(it.toInt()) }, 
                    valueRange = 1f..10f, 
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("CONNECTION TIMEOUT: ${connectionTimeout}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = connectionTimeout.toFloat(),
                    onValueChange = { onSetConnectionTimeout(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("MAX IMAGE SIZE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                val imageSizeOptions = listOf(262144 to "256KB", 524288 to "500KB", 1048576 to "1MB", 2097152 to "2MB")
                var imageSizeExpanded by remember { mutableStateOf(false) }
                val currentImageSizeLabel = imageSizeOptions.find { it.first == maxImageSize }?.second ?: "1MB"
                ExposedDropdownMenuBox(
                    expanded = imageSizeExpanded,
                    onExpandedChange = { imageSizeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentImageSizeLabel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = imageSizeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    ExposedDropdownMenu(
                        expanded = imageSizeExpanded,
                        onDismissRequest = { imageSizeExpanded = false }
                    ) {
                        imageSizeOptions.forEach { (bytes, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { onSetMaxImageSize(bytes); imageSizeExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ConsoleSection("SECURITY") {
                ConsoleSwitch("E2EE ENCRYPTION", isEncryptionEnabled, onToggleEncryption)
                Spacer(modifier = Modifier.height(12.dp))
                Text("BURN AFTER READING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 10, 30, 60).forEach { sec -> 
                        val selected = selfDestructSeconds == sec
                        Surface(
                            modifier = Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSetSelfDestruct(sec) },
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(
                                if (sec == 0) "OFF" else "${sec}S", 
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (selected) Color.Black else Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClearChat() }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red)
            ) {
                Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("PURGE ALL ARCHIVES")
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ConsoleSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color.White.copy(alpha = 0.02f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun ConsoleSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun TelemetryItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
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
