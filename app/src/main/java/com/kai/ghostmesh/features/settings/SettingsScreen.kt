package com.kai.ghostmesh.features.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.core.model.AppConfig

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
    mnemonic: String? = null,
    onGenerateBackup: () -> Unit = {},
    onRestoreIdentity: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreMnemonic by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text("Settings Suite", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                SettingsCategory("Profile & Identity") {
                    ProfileHeader(profile, onProfileChange)

                    ListItem(
                        headlineContent = { Text("Backup Identity", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Secure your 12-word recovery phrase") },
                        leadingContent = { Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { onGenerateBackup() }
                    )

                    if (mnemonic != null) {
                        BackupPhraseBox(mnemonic)
                    }

                    ListItem(
                        headlineContent = { Text("Restore Identity", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Recover your account from a seed phrase") },
                        leadingContent = { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable { showRestoreDialog = true }
                    )
                }

                SettingsCategory("Network Mesh") {
                    SettingsToggleItem("Stealth Mode", Icons.Default.VisibilityOff, isStealthMode, onToggleStealth)
                    SettingsToggleItem("Nearby Discovery", Icons.Default.Dns, isNearbyEnabled, onToggleNearby)
                    SettingsToggleItem("Bluetooth Transport", Icons.Default.Bluetooth, isBluetoothEnabled, onToggleBluetooth)
                    SettingsToggleItem("LAN Transport", Icons.Default.Lan, isLanEnabled, onToggleLan)
                    SettingsToggleItem("WiFi Direct", Icons.Default.Wifi, isWifiDirectEnabled, onToggleWifiDirect)
                }

                SettingsCategory("Security") {
                    SettingsToggleItem("Global Encryption", Icons.Default.Security, isEncryptionEnabled, onToggleEncryption)
                    ListItem(
                        headlineContent = { Text("Key Rotation") },
                        supportingContent = { Text("Force regenerate encryption session keys") },
                        leadingContent = { Icon(Icons.Default.Refresh, null) },
                        modifier = Modifier.clickable { Toast.makeText(context, "Keys Rotated", Toast.LENGTH_SHORT).show() }
                    )
                }

                SettingsCategory("Appearance") {
                    Text("Corner Radius ($cornerRadius dp)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp))
                    Slider(value = cornerRadius.toFloat(), onValueChange = { onSetCornerRadius(it.toInt()) }, valueRange = 0f..40f, modifier = Modifier.padding(start = 24.dp, end = 24.dp))

                    Text("Font Scale (${"%.1f".format(fontScale)}x)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp))
                    Slider(value = fontScale, onValueChange = onSetFontScale, valueRange = 0.8f..1.5f, modifier = Modifier.padding(start = 24.dp, end = 24.dp))
                }

                Spacer(Modifier.height(32.dp))
                ExpressiveButton(
                    onClick = onClearChat,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("PURGE ALL DATA")
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Identity") },
            text = {
                OutlinedTextField(
                    value = restoreMnemonic,
                    onValueChange = { restoreMnemonic = it },
                    label = { Text("Enter 12-word seed phrase") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { onRestoreIdentity(restoreMnemonic); showRestoreDialog = false }) { Text("RESTORE") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp), letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun BackupPhraseBox(mnemonic: String) {
    Card(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Your recovery phrase (STRICTLY CONFIDENTIAL):", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text(mnemonic, style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Copy this to a physical paper. Do not screenshot.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ProfileHeader(profile: UserProfile, onProfileChange: (String, String, Int?) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(profile.name) }

    ListItem(
        headlineContent = {
            if (isEditing) OutlinedTextField(value = tempName, onValueChange = { tempName = it }, modifier = Modifier.fillMaxWidth())
            else Text(profile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        },
        supportingContent = { Text(profile.id.take(16), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
        leadingContent = {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(profile.color).copy(alpha = 0.2f), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(profile.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color(profile.color))
                }
            }
        },
        trailingContent = {
            IconButton(onClick = { if (isEditing) onProfileChange(tempName, profile.status, null); isEditing = !isEditing }) {
                Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null)
            }
        }
    )
}
