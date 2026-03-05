package com.kai.ghostmesh

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.features.chat.ChatScreen
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.features.discovery.DiscoveryScreen
import com.kai.ghostmesh.features.discovery.DiscoveryViewModel
import com.kai.ghostmesh.features.docs.DocsScreen
import com.kai.ghostmesh.features.messages.MessagesScreen
import com.kai.ghostmesh.features.messages.MessagesViewModel
import com.kai.ghostmesh.features.settings.SettingsScreen
import com.kai.ghostmesh.features.settings.SettingsViewModel
import com.kai.ghostmesh.features.transfer.TransferHubScreen
import com.kai.ghostmesh.features.transfer.TransferViewModel
import com.kai.ghostmesh.service.MeshService
import com.kai.ghostmesh.core.ui.theme.ChateXTheme
import com.kai.ghostmesh.core.model.AppConfig

class MainActivity : ComponentActivity() {

    private var meshService: MeshService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeshService.MeshBinder
            meshService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            meshService = null
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.all { it.value }) startMesh()
        else Toast.makeText(this, "Permissions required for mesh networking", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        requestIgnoreBatteryOptimizations()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val discoveryViewModel: DiscoveryViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()
            val messagesViewModel: MessagesViewModel = viewModel()
            val transferViewModel: TransferViewModel = viewModel()

            val cornerRadiusSetting by settingsViewModel.cornerRadius.collectAsState()
            val fontScaleSetting by settingsViewModel.fontScale.collectAsState()
            val themeModeValue by settingsViewModel.themeMode.collectAsState()
            val isNearbyEnabled by settingsViewModel.isNearbyEnabled.collectAsState()
            val isBluetoothEnabled by settingsViewModel.isBluetoothEnabled.collectAsState()
            val isLanEnabled by settingsViewModel.isLanEnabled.collectAsState()
            val isWifiDirectEnabled by settingsViewModel.isWifiDirectEnabled.collectAsState()
            val encryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
            val stealthMode by settingsViewModel.isStealthMode.collectAsState()
            val currentSelfDestruct by settingsViewModel.selfDestructSeconds.collectAsState()
            val currentHopLimit by settingsViewModel.hopLimit.collectAsState()

            val onlinePeers by discoveryViewModel.connectedNodes.collectAsState()
            val meshHealth by discoveryViewModel.meshHealth.collectAsState()
            val userProfile by settingsViewModel.userProfile.collectAsState()
            val chatMessages by chatViewModel.messages.collectAsState()
            val typingPeers by chatViewModel.typingPeers.collectAsState()
            val replyToMessage by chatViewModel.replyToMessage.collectAsState()
            val activeChats by messagesViewModel.recentChats.collectAsState()
            val activeTransfers by transferViewModel.transfers.collectAsState()
            val mnemonic by settingsViewModel.mnemonic.collectAsState()

            ChateXTheme(cornerRadius = cornerRadiusSetting, fontScale = fontScaleSetting) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    val isMeshReady = remember { mutableStateOf(false) }
                    LaunchedEffect(isServiceBound) {
                        if (isServiceBound) isMeshReady.value = true
                    }

                    Column {
                        AnimatedVisibility(visible = !isMeshReady.value) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        NavHost(navController = navController, startDestination = "messages") {
                            composable("messages") {
                                MessagesScreen(
                                    chats = activeChats,
                                    meshHealth = meshHealth,
                                    onNavigateToChat = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                                    onNavigateToRadar = { navController.navigate("discovery") },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onRefresh = { /* Trigger refresh if needed */ },
                                    cornerRadius = cornerRadiusSetting
                                )
                            }
                            composable("discovery") {
                                DiscoveryScreen(
                                    connectedNodes = onlinePeers,
                                    meshHealth = meshHealth,
                                    cornerRadius = cornerRadiusSetting,
                                    onNodeClick = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                                    onShout = { discoveryViewModel.globalShout(it, encryptionEnabled, currentHopLimit, userProfile) }
                                )
                            }
                            composable("chat/{peerId}/{peerName}", arguments = listOf(navArgument("peerId") { type = NavType.StringType }, navArgument("peerName") { type = NavType.StringType })) { backStackEntry ->
                                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                                val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"

                                ChatScreen(
                                    peerId = peerId, peerName = peerName,
                                    messages = chatMessages,
                                    isTyping = typingPeers.contains(peerId),
                                    onSendMessage = { chatViewModel.sendMessage(it, encryptionEnabled, currentSelfDestruct, currentHopLimit, userProfile) },
                                    onSendImage = { uri: Uri -> chatViewModel.sendImage(uri, encryptionEnabled, currentSelfDestruct, currentHopLimit, userProfile) },
                                    onSendVideo = { uri: Uri -> chatViewModel.sendVideo(uri, encryptionEnabled, currentSelfDestruct, currentHopLimit, userProfile) },
                                    onStartVoice = { chatViewModel.startRecording() },
                                    onStopVoice = { chatViewModel.stopRecording() },
                                    onPlayVoice = { chatViewModel.playVoice(it) },
                                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                                    onTypingChange = { chatViewModel.sendTyping(it, userProfile) },
                                    onBack = { navController.popBackStack() },
                                    replyToMessage = replyToMessage,
                                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                                    onClearReply = { chatViewModel.clearReply() },
                                    cornerRadius = cornerRadiusSetting,
                                    transportType = onlinePeers[peerId]?.transportType
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    profile = userProfile,
                                    isDiscoveryEnabled = settingsViewModel.isDiscoveryEnabled.collectAsState().value,
                                    isAdvertisingEnabled = settingsViewModel.isAdvertisingEnabled.collectAsState().value,
                                    isStealthMode = stealthMode,
                                    isHapticEnabled = settingsViewModel.isHapticEnabled.collectAsState().value,
                                    isEncryptionEnabled = encryptionEnabled,
                                    selfDestructSeconds = currentSelfDestruct,
                                    hopLimit = currentHopLimit,
                                    packetsSent = settingsViewModel.packetsSent.collectAsState().value,
                                    packetsReceived = settingsViewModel.packetsReceived.collectAsState().value,
                                    animationSpeed = settingsViewModel.animationSpeed.collectAsState().value,
                                    hapticIntensity = settingsViewModel.hapticIntensity.collectAsState().value,
                                    messagePreview = settingsViewModel.messagePreview.collectAsState().value,
                                    autoReadReceipts = settingsViewModel.autoReadReceipts.collectAsState().value,
                                    compactMode = settingsViewModel.compactMode.collectAsState().value,
                                    showTimestamps = settingsViewModel.showTimestamps.collectAsState().value,
                                    connectionTimeout = settingsViewModel.connectionTimeout.collectAsState().value,
                                    scanInterval = settingsViewModel.scanInterval.collectAsState().value,
                                    maxImageSize = settingsViewModel.maxImageSize.collectAsState().value,
                                    themeMode = themeModeValue,
                                    cornerRadius = cornerRadiusSetting, fontScale = fontScaleSetting,
                                    isNearbyEnabled = isNearbyEnabled, isBluetoothEnabled = isBluetoothEnabled,
                                    isLanEnabled = isLanEnabled, isWifiDirectEnabled = isWifiDirectEnabled,
                                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                                    onToggleDiscovery = { settingsViewModel.updateSetting("discovery", it) },
                                    onToggleAdvertising = { settingsViewModel.updateSetting("advertising", it) },
                                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                                    onToggleHaptic = { settingsViewModel.updateSetting("haptic", it) },
                                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                                    onSetSelfDestruct = { settingsViewModel.updateSetting("self_destruct", it) },
                                    onSetHopLimit = { settingsViewModel.updateSetting(AppConfig.KEY_HOP_LIMIT, it) },
                                    onSetAnimationSpeed = { settingsViewModel.updateSetting("animation_speed", it) },
                                    onSetHapticIntensity = { settingsViewModel.updateSetting("haptic_intensity", it) },
                                    onToggleMessagePreview = { settingsViewModel.updateSetting("message_preview", it) },
                                    onToggleAutoReadReceipts = { settingsViewModel.updateSetting("auto_read_receipts", it) },
                                    onToggleCompactMode = { settingsViewModel.updateSetting("compact_mode", it) },
                                    onToggleShowTimestamps = { settingsViewModel.updateSetting("show_timestamps", it) },
                                    onSetConnectionTimeout = { settingsViewModel.updateSetting(AppConfig.KEY_CONN_TIMEOUT, it) },
                                    onSetScanInterval = { settingsViewModel.updateSetting(AppConfig.KEY_SCAN_INTERVAL, it) },
                                    onSetMaxImageSize = { settingsViewModel.updateSetting("max_image_size", it) },
                                    onSetThemeMode = { settingsViewModel.updateSetting("theme_mode", it) },
                                    onSetCornerRadius = { settingsViewModel.updateSetting(AppConfig.KEY_CORNER_RADIUS, it) },
                                    onSetFontScale = { settingsViewModel.updateSetting(AppConfig.KEY_FONT_SCALE, it) },
                                    packetCacheSize = 150,
                                    onSetPacketCache = { },
                                    onToggleNearby = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_NEARBY, it) },
                                    onToggleBluetooth = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_BLUETOOTH, it) },
                                    onToggleLan = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_LAN, it) },
                                    onToggleWifiDirect = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_WIFI_DIRECT, it) },
                                    onClearChat = { settingsViewModel.clearHistory() },
                                    onNavigateToDocs = { navController.navigate("docs") },
                                    onBack = { navController.popBackStack() },
                                    onNavigateToTransfers = { navController.navigate("transfer_hub") },
                                    mnemonic = mnemonic,
                                    onGenerateBackup = { settingsViewModel.generateBackupMnemonic() },
                                    onRestoreIdentity = { mnemonicString ->
                                        settingsViewModel.restoreIdentity(mnemonicString) { success ->
                                            if (success) Toast.makeText(this@MainActivity, "Identity Restored", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                            composable("transfer_hub") {
                                TransferHubScreen(
                                    transfers = activeTransfers,
                                    onCancel = { transferViewModel.cancelTransfer(it) },
                                    onBack = { navController.popBackStack() },
                                    cornerRadius = cornerRadiusSetting
                                )
                            }
                            composable("docs") {
                                DocsScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.addAll(listOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.POST_NOTIFICATIONS))
        }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startMesh() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startMesh() {
        if (isServiceBound) return
        val prefs = getSharedPreferences(com.kai.ghostmesh.core.model.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val intent = Intent(this, MeshService::class.java).apply {
            putExtra("NICKNAME", prefs.getString("nick", "Peer"))
            putExtra("STEALTH", prefs.getBoolean("stealth", false))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }
}
