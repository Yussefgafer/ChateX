package com.kai.ghostmesh

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.features.chat.ChatScreen
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.features.discovery.DiscoveryScreen
import com.kai.ghostmesh.features.discovery.DiscoveryViewModel
import com.kai.ghostmesh.features.messages.MessagesScreen
import com.kai.ghostmesh.features.messages.MessagesViewModel
import com.kai.ghostmesh.features.settings.SettingsScreen
import com.kai.ghostmesh.features.settings.SettingsViewModel
import com.kai.ghostmesh.service.MeshService
import com.kai.ghostmesh.core.ui.theme.ChateXTheme

class MainActivity : ComponentActivity() {

    private var meshService: MeshService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeshService.MeshBinder
            meshService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            meshService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serviceIntent = Intent(this, MeshService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            ChateXTheme(darkTheme = when(themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }) {
                val navController = rememberNavController()
                val chatViewModel: ChatViewModel = viewModel()
                val discoveryViewModel: DiscoveryViewModel = viewModel()
                val messagesViewModel: MessagesViewModel = viewModel()

                val isServiceReady by meshService?.isReady?.collectAsState() ?: remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isServiceReady) {
                        NavHost(navController = navController, startDestination = "messages") {
                            composable("messages") {
                                val chats by messagesViewModel.recentChats.collectAsState(emptyList())
                                val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val typingPeers by chatViewModel.typingPeers.collectAsState()
                                MessagesScreen(
                                    chats = chats,
                                    meshHealth = 1,
                                    onNavigateToChat = { peerId, peerName ->
                                        chatViewModel.setActiveChat(peerId)
                                        navController.navigate("chat/$peerId/$peerName")
                                    },
                                    onNavigateToRadar = { navController.navigate("discovery") },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onRefresh = {},
                                    cornerRadius = cornerRadius,
                                    typingPeers = typingPeers
                                )
                            }
                            composable("discovery") {
                                val connectedNodes by discoveryViewModel.connectedNodes.collectAsState()
                                val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                DiscoveryScreen(
                                    connectedNodes = connectedNodes,
                                    meshHealth = 1,
                                    cornerRadius = cornerRadius,
                                    onNodeClick = { peerId, peerName ->
                                        chatViewModel.setActiveChat(peerId)
                                        navController.navigate("chat/$peerId/$peerName")
                                    },
                                    onShout = { chatViewModel.sendMessage(it, true, 0, 5, com.kai.ghostmesh.core.model.UserProfile()) }
                                )
                            }
                            composable("settings") {
                                val profile by settingsViewModel.userProfile.collectAsState()
                                val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val fontScale by settingsViewModel.fontScale.collectAsState()
                                val isNearbyEnabled by settingsViewModel.isNearbyEnabled.collectAsState()
                                val isBluetoothEnabled by settingsViewModel.isBluetoothEnabled.collectAsState()
                                val isLanEnabled by settingsViewModel.isLanEnabled.collectAsState()
                                val isWifiDirectEnabled by settingsViewModel.isWifiDirectEnabled.collectAsState()
                                val isStealthMode by settingsViewModel.isStealthMode.collectAsState()
                                val isEncryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
                                val autoDownloadImages by settingsViewModel.autoDownloadImages.collectAsState()
                                val autoDownloadVideos by settingsViewModel.autoDownloadVideos.collectAsState()
                                val autoDownloadFiles by settingsViewModel.autoDownloadFiles.collectAsState()
                                val downloadSizeLimit by settingsViewModel.downloadSizeLimit.collectAsState()
                                val mnemonic by settingsViewModel.mnemonic.collectAsState()

                                val isDiscoveryEnabled by settingsViewModel.isDiscoveryEnabled.collectAsState()
                                val isAdvertisingEnabled by settingsViewModel.isAdvertisingEnabled.collectAsState()
                                val isHapticEnabled by settingsViewModel.isHapticEnabled.collectAsState()
                                val selfDestructSeconds by settingsViewModel.selfDestructSeconds.collectAsState()
                                val hopLimit by settingsViewModel.hopLimit.collectAsState()
                                val packetsSent by settingsViewModel.packetsSent.collectAsState()
                                val packetsReceived by settingsViewModel.packetsReceived.collectAsState()
                                val animationSpeed by settingsViewModel.animationSpeed.collectAsState()
                                val hapticIntensity by settingsViewModel.hapticIntensity.collectAsState()
                                val messagePreview by settingsViewModel.messagePreview.collectAsState()
                                val autoReadReceipts by settingsViewModel.autoReadReceipts.collectAsState()
                                val compactMode by settingsViewModel.compactMode.collectAsState()
                                val showTimestamps by settingsViewModel.showTimestamps.collectAsState()
                                val connectionTimeout by settingsViewModel.connectionTimeout.collectAsState()
                                val scanInterval by settingsViewModel.scanInterval.collectAsState()
                                val maxImageSize by settingsViewModel.maxImageSize.collectAsState()

                                SettingsScreen(
                                    profile = profile,
                                    isDiscoveryEnabled = isDiscoveryEnabled,
                                    isAdvertisingEnabled = isAdvertisingEnabled,
                                    isStealthMode = isStealthMode,
                                    isHapticEnabled = isHapticEnabled,
                                    isEncryptionEnabled = isEncryptionEnabled,
                                    selfDestructSeconds = selfDestructSeconds,
                                    hopLimit = hopLimit,
                                    packetsSent = packetsSent,
                                    packetsReceived = packetsReceived,
                                    animationSpeed = animationSpeed,
                                    hapticIntensity = hapticIntensity,
                                    messagePreview = messagePreview,
                                    autoReadReceipts = autoReadReceipts,
                                    compactMode = compactMode,
                                    showTimestamps = showTimestamps,
                                    connectionTimeout = connectionTimeout,
                                    scanInterval = scanInterval,
                                    maxImageSize = maxImageSize,
                                    themeMode = themeMode,
                                    cornerRadius = cornerRadius,
                                    fontScale = fontScale,
                                    isNearbyEnabled = isNearbyEnabled,
                                    isBluetoothEnabled = isBluetoothEnabled,
                                    isLanEnabled = isLanEnabled,
                                    isWifiDirectEnabled = isWifiDirectEnabled,
                                    autoDownloadImages = autoDownloadImages,
                                    autoDownloadVideos = autoDownloadVideos,
                                    autoDownloadFiles = autoDownloadFiles,
                                    downloadSizeLimit = downloadSizeLimit,
                                    mnemonic = mnemonic,
                                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                                    onToggleDiscovery = { settingsViewModel.updateSetting("discovery", it) },
                                    onToggleAdvertising = { settingsViewModel.updateSetting("advertising", it) },
                                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                                    onToggleHaptic = { settingsViewModel.updateSetting("haptic", it) },
                                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                                    onSetSelfDestruct = { settingsViewModel.updateSetting("self_destruct", it) },
                                    onSetHopLimit = { settingsViewModel.updateSetting("hop_limit", it) },
                                    onSetAnimationSpeed = { settingsViewModel.animationSpeed.value = it },
                                    onSetHapticIntensity = { settingsViewModel.hapticIntensity.value = it },
                                    onToggleMessagePreview = { settingsViewModel.messagePreview.value = it },
                                    onToggleAutoReadReceipts = { settingsViewModel.autoReadReceipts.value = it },
                                    onToggleCompactMode = { settingsViewModel.compactMode.value = it },
                                    onToggleShowTimestamps = { settingsViewModel.updateSetting("show_timestamps", it) },
                                    onSetConnectionTimeout = { settingsViewModel.connectionTimeout.value = it },
                                    onSetScanInterval = { settingsViewModel.scanInterval.value = it },
                                    onSetMaxImageSize = { settingsViewModel.maxImageSize.value = it },
                                    onToggleAutoDownloadImages = { settingsViewModel.updateSetting("auto_download_images", it) },
                                    onToggleAutoDownloadVideos = { settingsViewModel.updateSetting("auto_download_videos", it) },
                                    onToggleAutoDownloadFiles = { settingsViewModel.updateSetting("auto_download_files", it) },
                                    onSetDownloadSizeLimit = { settingsViewModel.updateSetting("download_size_limit", it) },
                                    onGenerateBackup = { settingsViewModel.generateBackupMnemonic() },
                                    onRestoreIdentity = { settingsViewModel.restoreIdentity(it) {} },
                                    onClearChat = { settingsViewModel.clearHistory() },
                                    onSetThemeMode = { settingsViewModel.updateSetting("theme_mode", it) },
                                    onSetCornerRadius = { settingsViewModel.updateSetting("corner_radius", it) },
                                    onSetFontScale = { settingsViewModel.updateSetting("font_scale", it) },
                                    onToggleNearby = { settingsViewModel.updateSetting("enable_nearby", it) },
                                    onToggleBluetooth = { settingsViewModel.updateSetting("enable_bluetooth", it) },
                                    onToggleLan = { settingsViewModel.updateSetting("enable_lan", it) },
                                    onToggleWifiDirect = { settingsViewModel.updateSetting("enable_wifi_direct", it) },
                                    onNavigateToDocs = {},
                                    onNavigateToTransfers = {},
                                    packetCacheSize = 150,
                                    onSetPacketCache = {},
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("chat/{peerId}/{peerName}", arguments = listOf(
                                navArgument("peerId") { type = NavType.StringType },
                                navArgument("peerName") { type = NavType.StringType }
                            )) { backStackEntry ->
                                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                                val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"

                                val chatMessages by chatViewModel.messages.collectAsState()
                                val stagedMedia by chatViewModel.stagedMedia.collectAsState()
                                val recordingDuration by chatViewModel.recordingDuration.collectAsState()
                                val currentCornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val encryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
                                val currentSelfDestruct by settingsViewModel.selfDestructSeconds.collectAsState()
                                val currentHopLimit by settingsViewModel.hopLimit.collectAsState()
                                val userProfile by settingsViewModel.userProfile.collectAsState()
                                val typingPeers by chatViewModel.typingPeers.collectAsState()
                                val replyToMessage by chatViewModel.replyToMessage.collectAsState()

                                ChatScreen(
                                    peerId = peerId, peerName = peerName,
                                    messages = chatMessages,
                                    isTyping = typingPeers.contains(peerId),
                                    onSendMessage = { chatViewModel.sendMessage(it, encryptionEnabled, currentSelfDestruct, currentHopLimit, userProfile) },
                                    onSendImage = { chatViewModel.stageMedia(it, ChatViewModel.MediaType.IMAGE) },
                                    onSendVideo = { chatViewModel.stageMedia(it, ChatViewModel.MediaType.VIDEO) },
                                    onStartVoice = { chatViewModel.startRecording() },
                                    onStopVoice = { chatViewModel.stopRecording() },
                                    onPlayVoice = { chatViewModel.playVoice(it) },
                                    onStopVoicePlayback = { chatViewModel.stopPlayback() },
                                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                                    onTypingChange = { chatViewModel.sendTyping(it, userProfile) },
                                    onBack = { navController.popBackStack() },
                                    replyToMessage = replyToMessage,
                                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                                    onClearReply = { chatViewModel.clearReply() },
                                    stagedMedia = stagedMedia,
                                    onStageMedia = { uri, type -> chatViewModel.stageMedia(uri, type) },
                                    onUnstageMedia = { chatViewModel.unstageMedia(it) },
                                    recordingDuration = recordingDuration,
                                    cornerRadius = currentCornerRadius,
                                    transportType = ""
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
