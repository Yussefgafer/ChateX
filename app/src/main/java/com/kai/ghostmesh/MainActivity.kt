package com.kai.ghostmesh

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.features.chat.*
import com.kai.ghostmesh.features.discovery.*
import com.kai.ghostmesh.features.settings.*
import com.kai.ghostmesh.features.messages.*
import com.kai.ghostmesh.features.maps.*
import com.kai.ghostmesh.features.docs.*
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.ui.theme.ChateXTheme
import com.kai.ghostmesh.service.MeshService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestIgnoreBatteryOptimizations()
        checkAndRequestPermissions()

        setContent {
            val messagesViewModel: MessagesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val discoveryViewModel: DiscoveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val themeMode by settingsViewModel.themeMode.collectAsState()

            ChateXTheme(darkTheme = when(themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        MainContent(
                            messagesViewModel, chatViewModel, discoveryViewModel, settingsViewModel,
                            navController
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        messagesViewModel: MessagesViewModel,
        chatViewModel: ChatViewModel,
        discoveryViewModel: DiscoveryViewModel,
        settingsViewModel: SettingsViewModel,
        navController: androidx.navigation.NavHostController
    ) {
        val recentChats by messagesViewModel.recentChats.collectAsState()
        val onlineGhosts by discoveryViewModel.connectedNodes.collectAsState()
        val meshHealth by discoveryViewModel.meshHealth.collectAsState()
        val routingTable by discoveryViewModel.routingTable.collectAsState()
        val locations by discoveryViewModel.neighborLocations.collectAsState()

        val userProfile by settingsViewModel.userProfile.collectAsState()
        val discoveryEnabled by settingsViewModel.isDiscoveryEnabled.collectAsState()
        val advertisingEnabled by settingsViewModel.isAdvertisingEnabled.collectAsState()
        val stealthMode by settingsViewModel.isStealthMode.collectAsState()
        val encryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
        val selfDestructSeconds by settingsViewModel.selfDestructSeconds.collectAsState()
        val hopLimit by settingsViewModel.hopLimit.collectAsState()
        val autoReadReceipts by settingsViewModel.autoReadReceipts.collectAsState()
        val themeMode by settingsViewModel.themeMode.collectAsState()
        val cornerRadiusSetting by settingsViewModel.cornerRadius.collectAsState()
        val fontScale by settingsViewModel.fontScale.collectAsState()
        val packetsSent by settingsViewModel.packetsSent.collectAsState()
        val packetsReceived by settingsViewModel.packetsReceived.collectAsState()
        val packetCacheSize by settingsViewModel.packetCacheSize.collectAsState()
        val isNearbyEnabled by settingsViewModel.isNearbyEnabled.collectAsState()
        val isBluetoothEnabled by settingsViewModel.isBluetoothEnabled.collectAsState()
        val isLanEnabled by settingsViewModel.isLanEnabled.collectAsState()
        val isWifiDirectEnabled by settingsViewModel.isWifiDirectEnabled.collectAsState()

        NavHost(
            navController = navController,
            startDestination = "messages",
            enterTransition = { fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            exitTransition = { fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            popEnterTransition = { fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            popExitTransition = { fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) }
        ) {
            composable("messages") {
                MessagesScreen(
                    recentChats = recentChats,
                    cornerRadius = cornerRadiusSetting,
                    onNavigateToChat = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("discovery") },
                    onNavigateToRadar = { navController.navigate("discovery") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onRefresh = { messagesViewModel.refreshConnections() }
                )
            }
            composable("discovery") {
                DiscoveryHub(
                    connectedNodes = onlineGhosts,
                    routingTable = routingTable,
                    meshHealth = meshHealth,
                    chatViewModel = chatViewModel,
                    cornerRadius = cornerRadiusSetting,
                    onShout = { discoveryViewModel.globalShout(it, encryptionEnabled, hopLimit, userProfile) }
                )
            }
            composable("settings") {
                SettingsScreen(
                    profile = userProfile, isDiscoveryEnabled = discoveryEnabled, isAdvertisingEnabled = advertisingEnabled,
                    isStealthMode = stealthMode, isHapticEnabled = true, isEncryptionEnabled = encryptionEnabled,
                    selfDestructSeconds = selfDestructSeconds, hopLimit = hopLimit,
                    packetsSent = packetsSent, packetsReceived = packetsReceived,
                    animationSpeed = 1.0f, hapticIntensity = 1,
                    messagePreview = true, autoReadReceipts = autoReadReceipts,
                    compactMode = false, showTimestamps = true,
                    connectionTimeout = 30, scanInterval = 10000L, maxImageSize = 2048, themeMode = themeMode,
                    cornerRadius = cornerRadiusSetting, fontScale = fontScale,
                    isNearbyEnabled = isNearbyEnabled, isBluetoothEnabled = isBluetoothEnabled,
                    isLanEnabled = isLanEnabled, isWifiDirectEnabled = isWifiDirectEnabled,
                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                    onToggleDiscovery = { settingsViewModel.updateSetting("discovery", it) },
                    onToggleAdvertising = { settingsViewModel.updateSetting("advertising", it) },
                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                    onToggleHaptic = { },
                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                    onSetSelfDestruct = { settingsViewModel.updateSetting("burn", it) },
                    onSetHopLimit = { settingsViewModel.updateSetting("hops", it) },
                    onSetAnimationSpeed = { },
                    onSetHapticIntensity = { },
                    onToggleMessagePreview = { },
                    onToggleAutoReadReceipts = { settingsViewModel.updateSetting("auto_read_receipts", it) },
                    onToggleCompactMode = { },
                    onToggleShowTimestamps = { },
                    onSetConnectionTimeout = { },
                    onSetScanInterval = { },
                    onSetMaxImageSize = { },
                    onSetThemeMode = { settingsViewModel.updateSetting("theme_mode", it) },
                    onSetCornerRadius = { settingsViewModel.updateSetting(AppConfig.KEY_CORNER_RADIUS, it) },
                    onSetFontScale = { settingsViewModel.updateSetting(AppConfig.KEY_FONT_SCALE, it) },
                    packetCacheSize = packetCacheSize,
                    onSetPacketCache = { settingsViewModel.updateSetting("net_packet_cache", it) },
                    onToggleNearby = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_NEARBY, it) },
                    onToggleBluetooth = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_BLUETOOTH, it) },
                    onToggleLan = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_LAN, it) },
                    onToggleWifiDirect = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_WIFI_DIRECT, it) },
                    onClearChat = { settingsViewModel.clearHistory() }, onBack = { navController.popBackStack() },
                    onSetProfileImage = { settingsViewModel.setProfileImage(it) },
                    onNavigateToDocs = { navController.navigate("docs") },
                    onNavigateToMaps = { navController.navigate("maps") },
                    onRestoreIdentity = { settingsViewModel.restoreIdentity(it) }
                )
            }
            composable("docs") { DocsScreen(onBack = { navController.popBackStack() }) }
            composable("maps") { MapsScreen(neighborLocations = locations, onBack = { navController.popBackStack() }) }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN); permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE); permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES); permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) startMesh()
        }
        launcher.launch(permissions.toTypedArray())
    }

    private fun startMesh() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val nickname = prefs.getString("nick", "Ghost")!!
        val isStealth = prefs.getBoolean("stealth", false)
        val intent = Intent(this, MeshService::class.java).apply {
            putExtra("NICKNAME", nickname)
            putExtra("STEALTH", isStealth)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    @androidx.annotation.OptIn(androidx.core.os.BuildCompat.PrereleaseSdkCheck::class)
    @android.annotation.SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try { startActivity(intent) } catch (e: Exception) { /* User denied */ }
            }
        }
    }
}
