package com.kai.ghostmesh.features.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.IdentityManager
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val container = (application as? GhostApplication)?.container 
        ?: (application.applicationContext as? GhostApplication)?.container

    private val meshManager: MeshManager? = container?.meshManager
    private val repository: GhostRepository? = container?.repository
    private val prefs = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(UserProfile(
        id = container?.myNodeId ?: "GHOST", 
        name = prefs.getString("nick", "Ghost")!!, 
        status = prefs.getString("status", "Available")!!,
        color = prefs.getInt("soul_color", 0xFF00FF7F.toInt())
    ))
    val userProfile = _userProfile.asStateFlow()

    private val _mnemonic = MutableStateFlow<String?>(null)
    val mnemonic = _mnemonic.asStateFlow()

    val cornerRadius = MutableStateFlow(prefs.getInt(AppConfig.KEY_CORNER_RADIUS, AppConfig.DEFAULT_CORNER_RADIUS))
    val fontScale = MutableStateFlow(prefs.getFloat(AppConfig.KEY_FONT_SCALE, AppConfig.DEFAULT_FONT_SCALE))
    val isStealthMode = MutableStateFlow(prefs.getBoolean("stealth", false))
    val isEncryptionEnabled = MutableStateFlow(prefs.getBoolean("encryption", true))
    val selfDestructSeconds = MutableStateFlow(prefs.getInt("self_destruct", 0))
    val hopLimit = MutableStateFlow(prefs.getInt(AppConfig.KEY_HOP_LIMIT, AppConfig.DEFAULT_HOP_LIMIT))

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

    val isNearbyEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true))
    val isBluetoothEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true))
    val isLanEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true))
    val isWifiDirectEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true))

    val packetsSent = meshManager?.totalPacketsSent ?: MutableStateFlow(0)
    val packetsReceived = meshManager?.totalPacketsReceived ?: MutableStateFlow(0)

    val scanInterval = MutableStateFlow(prefs.getLong(AppConfig.KEY_SCAN_INTERVAL, AppConfig.DEFAULT_SCAN_INTERVAL_MS))

    fun updateMyProfile(name: String, status: String, colorHex: Int? = null) {
        val current = _userProfile.value
        val updated = current.copy(name = name, status = status, color = colorHex ?: current.color)
        _userProfile.value = updated
        prefs.edit().putString("nick", name).putString("status", status).putInt("soul_color", updated.color).apply()
    }

    fun generateBackupMnemonic() {
        viewModelScope.launch {
            _mnemonic.value = IdentityManager.generateMnemonic()
        }
    }

    fun restoreIdentity(mnemonic: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val success = SecurityManager.recoverIdentity(mnemonic)
            if (success) {
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun updateSetting(key: String, value: Any) {
        when(key) {
            AppConfig.KEY_CORNER_RADIUS -> cornerRadius.value = value as Int
            AppConfig.KEY_FONT_SCALE -> fontScale.value = value as Float
            "stealth" -> isStealthMode.value = value as Boolean
            "encryption" -> isEncryptionEnabled.value = value as Boolean
            "self_destruct" -> selfDestructSeconds.value = value as Int
            AppConfig.KEY_HOP_LIMIT -> hopLimit.value = value as Int
            "discovery" -> isDiscoveryEnabled.value = value as Boolean
            "advertising" -> isAdvertisingEnabled.value = value as Boolean
            "haptic" -> isHapticEnabled.value = value as Boolean
            "animation_speed" -> animationSpeed.value = value as Float
            "haptic_intensity" -> hapticIntensity.value = value as Int
            "message_preview" -> messagePreview.value = value as Boolean
            "auto_read_receipts" -> autoReadReceipts.value = value as Boolean
            "compact_mode" -> compactMode.value = value as Boolean
            "show_timestamps" -> showTimestamps.value = value as Boolean
            AppConfig.KEY_CONN_TIMEOUT -> connectionTimeout.value = value as Int
            "max_image_size" -> maxImageSize.value = value as Int
            "theme_mode" -> themeMode.value = value as Int
            AppConfig.KEY_ENABLE_NEARBY -> isNearbyEnabled.value = value as Boolean
            AppConfig.KEY_ENABLE_BLUETOOTH -> isBluetoothEnabled.value = value as Boolean
            AppConfig.KEY_ENABLE_LAN -> isLanEnabled.value = value as Boolean
            AppConfig.KEY_ENABLE_WIFI_DIRECT -> isWifiDirectEnabled.value = value as Boolean
        }

        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().apply {
                when(value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                }
                apply()
            }
        }
    }

    fun clearHistory() = viewModelScope.launch { repository?.purgeArchives() }
}
