package com.kai.ghostmesh

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.ui.*
import com.kai.ghostmesh.ui.theme.ChateXTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GhostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            viewModel.showError("Google Play Services missing or outdated. Mesh networking disabled.")
        }
        
        checkAndRequestPermissions()
        requestIgnoreBatteryOptimizations()

        setContent {
            val errorMessage by viewModel.errorMessage.collectAsState()
            val userProfile by viewModel.userProfile.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            
            val isDarkTheme = when(themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ChateXTheme(
                darkTheme = isDarkTheme,
                dynamicColor = true,
                userPrimaryColor = userProfile.color
            ) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                
                LaunchedEffect(errorMessage) {
                    errorMessage?.let { message ->
                        snackbarHostState.showSnackbar(message)
                        viewModel.clearErrorMessage()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val showBottomBar = currentDestination?.route in listOf("messages", "discovery", "settings")
                        
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
                            NavigationBar {
                                val items = listOf(
                                    Triple("discovery", Icons.Filled.DeviceHub, Icons.Outlined.DeviceHub),
                                    Triple("messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubble),
                                    Triple("settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                                )
                                
                                items.forEach { (route, selectedIcon, unselectedIcon) ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true
                                    NavigationBarItem(
                                        icon = { Icon(if (isSelected) selectedIcon else unselectedIcon, contentDescription = route) },
                                        label = { Text(route.replaceFirstChar { it.uppercase() }) },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        MainContent(viewModel, navController)
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent(viewModel: GhostViewModel, navController: androidx.navigation.NavHostController) {
        val chatHistory by viewModel.activeChatHistory.collectAsState()
        val onlineGhosts by viewModel.onlineGhosts.collectAsState()
        val typingGhosts by viewModel.typingGhosts.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        
        val discoveryEnabled by viewModel.isDiscoveryEnabled.collectAsState()
        val advertisingEnabled by viewModel.isAdvertisingEnabled.collectAsState()
        val stealthMode by viewModel.isStealthMode.collectAsState()
        val hapticEnabled by viewModel.isHapticEnabled.collectAsState()
        val encryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
        val selfDestructSeconds by viewModel.selfDestructSeconds.collectAsState()
        val hopLimit by viewModel.hopLimit.collectAsState()
        
        val animationSpeed by viewModel.animationSpeed.collectAsState()
        val hapticIntensity by viewModel.hapticIntensity.collectAsState()
        val messagePreview by viewModel.messagePreview.collectAsState()
        val autoReadReceipts by viewModel.autoReadReceipts.collectAsState()
        val compactMode by viewModel.compactMode.collectAsState()
        val showTimestamps by viewModel.showTimestamps.collectAsState()
        val connectionTimeout by viewModel.connectionTimeout.collectAsState()
        val maxImageSize by viewModel.maxImageSize.collectAsState()
        val themeMode by viewModel.themeMode.collectAsState()
        
        val packetsSent by viewModel.packetsSent.collectAsState()
        val packetsReceived by viewModel.packetsReceived.collectAsState()
        val meshHealth by viewModel.meshHealth.collectAsState()
        val replyToMessage by viewModel.replyToMessage.collectAsState()

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(
                navController = navController, 
                startDestination = "messages",
                enterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                exitTransition = { fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                popExitTransition = { fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
            ) {
                composable("messages") {
                    val recentChats by viewModel.recentChats.collectAsState()
                    MessagesScreen(
                        recentChats = recentChats,
                        onNavigateToChat = { id, name -> viewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                        onNavigateToRadar = { navController.navigate("discovery") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onRefresh = { viewModel.refreshConnections() }
                    )
                }
                composable("discovery") {
                    DiscoveryScreen(
                        connectedNodes = onlineGhosts,
                        meshHealth = meshHealth,
                        onNodeClick = { id, name -> viewModel.setActiveChat(id); navController.navigate("chat/$id/$name") }
                    )
                }
                composable("chat/{ghostId}/{ghostName}", arguments = listOf(navArgument("ghostId") { type = NavType.StringType }, navArgument("ghostName") { type = NavType.StringType })) { backStackEntry ->
                    val ghostId = backStackEntry.arguments?.getString("ghostId") ?: ""
                    val ghostName = backStackEntry.arguments?.getString("ghostName") ?: "Unknown"
                    ChatScreen(
                        ghostId = ghostId, ghostName = ghostName, messages = chatHistory, 
                        isTyping = typingGhosts.contains(ghostId), 
                        onSendMessage = { viewModel.sendMessage(it) }, 
                        onSendImage = { uri: Uri -> viewModel.sendImage(uri) }, 
                        onStartVoice = { viewModel.startRecording() }, 
                        onStopVoice = { viewModel.stopRecording() }, 
                        onPlayVoice = { viewModel.playVoice(it) }, 
                        onDeleteMessage = { viewModel.deleteMessage(it) },
                        onTypingChange = { viewModel.sendTyping(it) }, 
                        onBack = { viewModel.setActiveChat(null); viewModel.clearReply(); navController.popBackStack() },
                        replyToMessage = replyToMessage,
                        onSetReply = { id, content, sender -> viewModel.setReplyTo(id, content, sender) },
                        onClearReply = { viewModel.clearReply() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        profile = userProfile, isDiscoveryEnabled = discoveryEnabled, isAdvertisingEnabled = advertisingEnabled,
                        isStealthMode = stealthMode, isHapticEnabled = hapticEnabled, isEncryptionEnabled = encryptionEnabled, 
                        selfDestructSeconds = selfDestructSeconds, hopLimit = hopLimit, 
                        packetsSent = packetsSent, packetsReceived = packetsReceived,
                        animationSpeed = animationSpeed, hapticIntensity = hapticIntensity,
                        messagePreview = messagePreview, autoReadReceipts = autoReadReceipts,
                        compactMode = compactMode, showTimestamps = showTimestamps,
                        connectionTimeout = connectionTimeout, maxImageSize = maxImageSize, themeMode = themeMode,
                        onProfileChange = { n, s, c -> viewModel.updateMyProfile(n, s, c) },
                        onToggleDiscovery = { viewModel.isDiscoveryEnabled.value = it; viewModel.updateSetting("discovery", it) }, 
                        onToggleAdvertising = { viewModel.isAdvertisingEnabled.value = it; viewModel.updateSetting("advertising", it) },
                        onToggleStealth = { viewModel.isStealthMode.value = it; viewModel.updateSetting("stealth", it) },
                        onToggleHaptic = { viewModel.isHapticEnabled.value = it; viewModel.updateSetting("haptic", it) }, 
                        onToggleEncryption = { viewModel.isEncryptionEnabled.value = it; viewModel.updateSetting("encryption", it) },
                        onSetSelfDestruct = { viewModel.selfDestructSeconds.value = it; viewModel.updateSetting("burn", it) }, 
                        onSetHopLimit = { viewModel.hopLimit.value = it; viewModel.updateSetting("hops", it) },
                        onSetAnimationSpeed = { viewModel.animationSpeed.value = it; viewModel.updateSetting("animation_speed", it) },
                        onSetHapticIntensity = { viewModel.hapticIntensity.value = it; viewModel.updateSetting("haptic_intensity", it) },
                        onToggleMessagePreview = { viewModel.messagePreview.value = it; viewModel.updateSetting("message_preview", it) },
                        onToggleAutoReadReceipts = { viewModel.autoReadReceipts.value = it; viewModel.updateSetting("auto_read_receipts", it) },
                        onToggleCompactMode = { viewModel.compactMode.value = it; viewModel.updateSetting("compact_mode", it) },
                        onToggleShowTimestamps = { viewModel.showTimestamps.value = it; viewModel.updateSetting("show_timestamps", it) },
                        onSetConnectionTimeout = { viewModel.connectionTimeout.value = it; viewModel.updateSetting("connection_timeout", it) },
                        onSetMaxImageSize = { viewModel.maxImageSize.value = it; viewModel.updateSetting("max_image_size", it) },
                        onSetThemeMode = { viewModel.themeMode.value = it; viewModel.updateSetting("theme_mode", it) },
                        onClearChat = { viewModel.clearHistory() }, onBack = { navController.popBackStack() }
                    )
                }
            }
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
            if (results.all { it.value }) viewModel.startMesh()
        }
        launcher.launch(permissions.toTypedArray())
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
