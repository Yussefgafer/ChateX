package com.kai.ghostmesh.features.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GhostApplication).container
    private val meshManager = container.meshManager
    private val repository = container.repository
    private val prefs = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(UserProfile(id = container.myNodeId, name = prefs.getString("nick", "Ghost")!!, status = prefs.getString("status", "Roaming the void")!!, color = prefs.getInt("soul_color", 0xFF00FF7F.toInt())))
    val userProfile = _userProfile.asStateFlow()

    // Configurable parameters via AppConfig
    val cornerRadius = MutableStateFlow(prefs.getInt(AppConfig.KEY_CORNER_RADIUS, AppConfig.DEFAULT_CORNER_RADIUS))
    val fontScale = MutableStateFlow(prefs.getFloat(AppConfig.KEY_FONT_SCALE, AppConfig.DEFAULT_FONT_SCALE))
    val scanInterval = MutableStateFlow(prefs.getLong(AppConfig.KEY_SCAN_INTERVAL, AppConfig.DEFAULT_SCAN_INTERVAL_MS))
    val hopLimit = MutableStateFlow(prefs.getInt(AppConfig.KEY_HOP_LIMIT, AppConfig.DEFAULT_HOP_LIMIT))
    val isStealthMode = MutableStateFlow(prefs.getBoolean("stealth", false))
    val isEncryptionEnabled = MutableStateFlow(prefs.getBoolean("encryption", true))
    val selfDestructSeconds = MutableStateFlow(prefs.getInt("self_destruct", 0))

    val isDiscoveryEnabled = MutableStateFlow(prefs.getBoolean("discovery", true))
    val isAdvertisingEnabled = MutableStateFlow(prefs.getBoolean("advertising", true))
    val isHapticEnabled = MutableStateFlow(prefs.getBoolean("haptic", true))
    val animationSpeed = MutableStateFlow(prefs.getFloat("animation_speed", 1.0f))
    val hapticIntensity = MutableStateFlow(prefs.getInt("haptic_intensity", 1))
    val messagePreview = MutableStateFlow(prefs.getBoolean("message_preview", true))
    val autoReadReceipts = MutableStateFlow(prefs.getBoolean("auto_read_receipts", true))
    val compactMode = MutableStateFlow(prefs.getBoolean("compact_mode", false))
    val showTimestamps = MutableStateFlow(prefs.getBoolean("show_timestamps", true))
    val connectionTimeout = MutableStateFlow(prefs.getInt(AppConfig.KEY_CONN_TIMEOUT, AppConfig.DEFAULT_CONNECTION_TIMEOUT_S))
    val maxImageSize = MutableStateFlow(prefs.getInt("max_image_size", 2048))
    val themeMode = MutableStateFlow(prefs.getInt("theme_mode", 0))
    val packetCacheSize = MutableStateFlow(prefs.getInt("net_packet_cache", 2000))
    val isNearbyEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true))
    val isBluetoothEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true))
    val isLanEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true))
    val isWifiDirectEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true))

    val packetsSent = meshManager.totalPacketsSent
    val packetsReceived = meshManager.totalPacketsReceived

    fun updateMyProfile(name: String, status: String, colorHex: Int? = null) {
        val current = _userProfile.value
        _userProfile.value = current.copy(name = name, status = status, color = colorHex ?: current.color)
        prefs.edit().putString("nick", name).putString("status", status).putInt("soul_color", _userProfile.value.color).apply()
        syncProfile()
    }

    fun updateSetting(key: String, value: Any) {
        prefs.edit().apply {
            when(value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
            }; apply()
        }
        if (key == "stealth" || key.startsWith("net_enable_") || key == "discovery" || key == "advertising") {
            meshManager.stop()
            meshManager.startMesh(_userProfile.value.name, isStealthMode.value)
        }
    }

    private fun syncProfile() {
        if (isStealthMode.value) return
        val profile = _userProfile.value
        meshManager.sendPacket(Packet(senderId = container.myNodeId, senderName = profile.name, type = PacketType.PROFILE_SYNC, payload = "${profile.name}|${profile.status}|${profile.color}"))
        SecurityManager.getMyPublicKey()?.let { pubKey ->
            meshManager.sendPacket(Packet(senderId = container.myNodeId, senderName = profile.name, type = PacketType.KEY_EXCHANGE, payload = pubKey))
        }
    }

    fun clearHistory() = viewModelScope.launch { repository.purgeArchives() }
}
