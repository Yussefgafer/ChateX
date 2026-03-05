package com.kai.ghostmesh.features.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*

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
    scanInterval: Long,
    maxImageSize: Int,
    themeMode: Int,
    cornerRadius: Int,
    fontScale: Float,
    isNearbyEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    isLanEnabled: Boolean,
    isWifiDirectEnabled: Boolean,
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
    onSetScanInterval: (Long) -> Unit,
    onSetMaxImageSize: (Int) -> Unit,
    onSetThemeMode: (Int) -> Unit,
    onSetCornerRadius: (Int) -> Unit,
    onSetFontScale: (Float) -> Unit,
    packetCacheSize: Int,
    onSetPacketCache: (Int) -> Unit,
    onToggleNearby: (Boolean) -> Unit,
    onToggleBluetooth: (Boolean) -> Unit,
    onToggleLan: (Boolean) -> Unit,
    onToggleWifiDirect: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onNavigateToDocs: () -> Unit,
    onBack: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    mnemonic: String?,
    onGenerateBackup: () -> Unit,
    onRestoreIdentity: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreMnemonic by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                MediumTopAppBar(
                    title = { Text("CONTROL CENTER", fontWeight = FontWeight.Black) },
                    actions = {
                        Text("v1.0.0", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Identity Management Segment
                SettingsCategory("IDENTITY") {
                    CoercedExpressiveCard(cornerRadius.toFloat(), modifier = Modifier.fillMaxWidth()) {
                        ProfileHeader(profile, onProfileChange)
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveButton(onClick = onGenerateBackup, modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("BACKUP", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                            }
                            ExpressiveButton(onClick = { showRestoreDialog = true }, modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("RESTORE", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (mnemonic != null) {
                        Spacer(Modifier.height(16.dp))
                        BackupPhraseBox(mnemonic, cornerRadius.toFloat())
                    }
                }

                // Network Protocol Segment
                SettingsCategory("NETWORK PROTOCOLS") {
                    CoercedExpressiveCard(cornerRadius.toFloat(), modifier = Modifier.fillMaxWidth()) {
                        SettingsToggleItem("Stealth Mode", Icons.Default.VisibilityOff, isStealthMode, onToggleStealth)
                        SettingsToggleItem("Nearby Discovery", Icons.Default.Dns, isNearbyEnabled, onToggleNearby)
                        SettingsToggleItem("Bluetooth Mesh", Icons.Default.Bluetooth, isBluetoothEnabled, onToggleBluetooth)
                        SettingsToggleItem("LAN Transport", Icons.Default.Lan, isLanEnabled, onToggleLan)
                        SettingsToggleItem("WiFi Direct", Icons.Default.Wifi, isWifiDirectEnabled, onToggleWifiDirect)
                    }
                }

                // Security Standards
                SettingsCategory("SECURITY") {
                    CoercedExpressiveCard(cornerRadius.toFloat(), modifier = Modifier.fillMaxWidth()) {
                        SettingsToggleItem("End-to-End Encryption", Icons.Default.Security, isEncryptionEnabled, onToggleEncryption)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClearChat() }.padding(vertical = 8.dp)) {
                            Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(16.dp))
                            Text("Purge Local Repository", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // UI Expressiveness
                SettingsCategory("INTERFACE") {
                    CoercedExpressiveCard(cornerRadius.toFloat(), modifier = Modifier.fillMaxWidth()) {
                        Text("Geometric Curvature", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        ExpressiveSlider(value = cornerRadius.toFloat(), onValueChange = { onSetCornerRadius(it.toInt()) }, valueRange = 0f..100f)
                        Spacer(Modifier.height(12.dp))
                        Text("Typography Scale", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        ExpressiveSlider(value = fontScale, onValueChange = onSetFontScale, valueRange = 0.8f..1.5f)
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("RESTORE IDENTITY", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = restoreMnemonic,
                    onValueChange = { restoreMnemonic = it },
                    label = { Text("12-word recovery mnemonic") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                ExpressiveButton(onClick = { onRestoreIdentity(restoreMnemonic); showRestoreDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("CONFIRM") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("CANCEL") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp).alpha(0.7f))
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) { { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) } } else null
        )
    }
}

@Composable
fun BackupPhraseBox(mnemonic: String, userRadius: Float) {
    CoercedExpressiveCard(userRadius, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("RECOVERY MNEMONIC", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Text(mnemonic, style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
        }
    }
}

@Composable
fun ProfileHeader(profile: UserProfile, onProfileChange: (String, String, Int?) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(profile.name) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = Color(profile.color).copy(alpha = 0.15f),
            modifier = Modifier.size(64.dp).border(1.dp, Color(profile.color).copy(alpha = 0.3f), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(profile.name.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge, color = Color(profile.color), fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (isEditing) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(profile.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text(profile.id.take(16).uppercase() + "...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        ExpressiveIconButton(
            onClick = {
                if (isEditing) onProfileChange(tempName, profile.status, null)
                isEditing = !isEditing
            },
            containerColor = if (isEditing) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        ) {
            Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null)
        }
    }
}
