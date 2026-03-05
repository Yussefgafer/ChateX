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
import com.kai.ghostmesh.ui.theme.GhostMeshTheme

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

            GhostMeshTheme(darkTheme = when(themeMode) {
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
                                MessagesScreen(
                                    chats = chats,
                                    onChatClick = { peerId, peerName ->
                                        chatViewModel.setActiveChat(peerId)
                                        navController.navigate("chat/$peerId/$peerName")
                                    },
                                    onDiscoveryClick = { navController.navigate("discovery") },
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            }
                            composable("discovery") {
                                val connectedNodes by discoveryViewModel.connectedNodes.collectAsState()
                                val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                DiscoveryScreen(
                                    connectedNodes = connectedNodes,
                                    cornerRadius = cornerRadius,
                                    onNodeClick = { peerId, peerName ->
                                        chatViewModel.setActiveChat(peerId)
                                        navController.navigate("chat/$peerId/$peerName")
                                    },
                                    onShout = { discoveryViewModel.shout(it) }
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

                                SettingsScreen(
                                    profile = profile,
                                    isStealthMode = isStealthMode,
                                    isEncryptionEnabled = isEncryptionEnabled,
                                    autoDownloadImages = autoDownloadImages,
                                    autoDownloadVideos = autoDownloadVideos,
                                    autoDownloadFiles = autoDownloadFiles,
                                    downloadSizeLimit = downloadSizeLimit,
                                    mnemonic = mnemonic,
                                    themeMode = themeMode,
                                    cornerRadius = cornerRadius,
                                    fontScale = fontScale,
                                    isNearbyEnabled = isNearbyEnabled,
                                    isBluetoothEnabled = isBluetoothEnabled,
                                    isLanEnabled = isLanEnabled,
                                    isWifiDirectEnabled = isWifiDirectEnabled,
                                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                                    onToggleAutoDownloadImages = { settingsViewModel.updateSetting("auto_download_images", it) },
                                    onToggleAutoDownloadVideos = { settingsViewModel.updateSetting("auto_download_videos", it) },
                                    onToggleAutoDownloadFiles = { settingsViewModel.updateSetting("auto_download_files", it) },
                                    onSetDownloadSizeLimit = { settingsViewModel.updateSetting("download_size_limit", it) },
                                    onGenerateBackup = { settingsViewModel.generateBackupMnemonic() },
                                    onClearChat = { settingsViewModel.clearHistory() },
                                    onSetTheme = { settingsViewModel.updateSetting("theme_mode", it) },
                                    onSetCornerRadius = { settingsViewModel.updateSetting("corner_radius", it) },
                                    onSetFontScale = { settingsViewModel.updateSetting("font_scale", it) },
                                    onToggleNearby = { settingsViewModel.updateSetting("enable_nearby", it) },
                                    onToggleBluetooth = { settingsViewModel.updateSetting("enable_bluetooth", it) },
                                    onToggleLan = { settingsViewModel.updateSetting("enable_lan", it) },
                                    onToggleWifiDirect = { settingsViewModel.updateSetting("enable_wifi_direct", it) },
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
