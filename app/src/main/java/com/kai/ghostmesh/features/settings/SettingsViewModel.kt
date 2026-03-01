package com.kai.ghostmesh.features.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GhostApplication).container
    private val meshManager = container.meshManager
    private val repository = container.repository
    private val prefs = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(UserProfile(
        id = container.myNodeId,
        name = prefs.getString("nick", "Ghost")!!,
        status = prefs.getString("status", "Roaming the void")!!,
        color = prefs.getInt("soul_color", 0xFF00FF7F.toInt()),
        profileImageLocalPath = prefs.getString("profile_image_path", null)
    ))
    val userProfile = _userProfile.asStateFlow()

    val cornerRadius = MutableStateFlow(prefs.getInt(AppConfig.KEY_CORNER_RADIUS, AppConfig.DEFAULT_CORNER_RADIUS))
    val fontScale = MutableStateFlow(prefs.getFloat(AppConfig.KEY_FONT_SCALE, AppConfig.DEFAULT_FONT_SCALE))
    val scanInterval = MutableStateFlow(prefs.getLong(AppConfig.KEY_SCAN_INTERVAL, AppConfig.DEFAULT_SCAN_INTERVAL_MS))
    val hopLimit = MutableStateFlow(prefs.getInt(AppConfig.KEY_HOP_LIMIT, AppConfig.DEFAULT_HOP_LIMIT))
    val isStealthMode = MutableStateFlow(prefs.getBoolean("stealth", false))
    val isEncryptionEnabled = MutableStateFlow(prefs.getBoolean("encryption", true))
    val selfDestructSeconds = MutableStateFlow(prefs.getInt("self_destruct", 0))

    val isDiscoveryEnabled = MutableStateFlow(prefs.getBoolean("discovery", true))
    val isAdvertisingEnabled = MutableStateFlow(prefs.getBoolean("advertising", true))
    val hapticIntensity = MutableStateFlow(prefs.getInt("haptic_intensity", 1))
    val autoReadReceipts = MutableStateFlow(prefs.getBoolean("auto_read_receipts", true))
    val themeMode = MutableStateFlow(prefs.getInt("theme_mode", 0))
    val packetCacheSize = MutableStateFlow(prefs.getInt("net_packet_cache", 2000))
    val isNearbyEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true))
    val isBluetoothEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true))
    val isLanEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true))
    val isWifiDirectEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true))

    // Mission Hardening: Adding missing delegates
    val isHapticEnabled = MutableStateFlow(prefs.getBoolean("haptic", true))
    val animationSpeed = MutableStateFlow(prefs.getFloat("animation_speed", 1.0f))
    val messagePreview = MutableStateFlow(prefs.getBoolean("message_preview", true))
    val compactMode = MutableStateFlow(prefs.getBoolean("compact_mode", false))
    val showTimestamps = MutableStateFlow(prefs.getBoolean("show_timestamps", true))
    val connectionTimeout = MutableStateFlow(prefs.getInt(AppConfig.KEY_CONN_TIMEOUT, AppConfig.DEFAULT_CONNECTION_TIMEOUT_S))
    val maxImageSize = MutableStateFlow(prefs.getInt("max_image_size", 2048))

    val packetsSent = meshManager.totalPacketsSent
    val packetsReceived = meshManager.totalPacketsReceived

    fun updateMyProfile(name: String, status: String, colorHex: Int? = null) {
        val current = _userProfile.value
        _userProfile.value = current.copy(name = name, status = status, color = colorHex ?: current.color)
        prefs.edit().putString("nick", name).putString("status", status).putInt("soul_color", _userProfile.value.color).apply()
        syncProfile()
    }

    fun setProfileImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                var quality = 80
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                while (outputStream.size() > 51200 && quality > 10) {
                    outputStream.reset()
                    quality -= 10
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                }
                val bytes = outputStream.toByteArray()
                val file = File(getApplication<Application>().filesDir, "my_avatar.jpg")
                FileOutputStream(file).use { it.write(bytes) }
                _userProfile.value = _userProfile.value.copy(profileImageLocalPath = file.absolutePath)
                prefs.edit().putString("profile_image_path", file.absolutePath).apply()
                val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                meshManager.sendPacket(Packet(senderId = container.myNodeId, senderName = _userProfile.value.name, type = PacketType.PROFILE_IMAGE, payload = base64Image))
            } catch (e: Exception) {}
        }
    }

    fun restoreIdentity(mnemonic: String) {
        try {
            val words = mnemonic.trim().split(Regex("\\s+"))
            SecurityManager.restoreIdentity(words)
            _userProfile.value = _userProfile.value.copy(id = SecurityManager.getNostrPublicKey())
            meshManager.stop()
            meshManager.startMesh(_userProfile.value.name, isStealthMode.value)
        } catch (e: Exception) { }
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
