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
        id = SecurityManager.getNostrPublicKey(),
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
        viewModelScope.launch(Dispatchers.Default) {
            _mnemonic.value = IdentityManager.generateMnemonic()
        }
    }

    fun restoreIdentity(mnemonic: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val success = SecurityManager.recoverIdentity(mnemonic)
            if (success) {
                val newNodeId = SecurityManager.getNostrPublicKey()
                _userProfile.value = _userProfile.value.copy(id = newNodeId)
                withContext(Dispatchers.Main) {
                    meshManager?.stop()
                    meshManager?.startMesh(_userProfile.value.name, isStealthMode.value)
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
            AppConfig.KEY_CORNER_RADIUS -> (value as? Int)?.let { cornerRadius.value = it }
            AppConfig.KEY_FONT_SCALE -> (value as? Float)?.let { fontScale.value = it }
            "stealth" -> (value as? Boolean)?.let { isStealthMode.value = it }
            "encryption" -> (value as? Boolean)?.let { isEncryptionEnabled.value = it }
            "self_destruct" -> (value as? Int)?.let { selfDestructSeconds.value = it }
            AppConfig.KEY_HOP_LIMIT -> (value as? Int)?.let { hopLimit.value = it }
            "discovery" -> (value as? Boolean)?.let { isDiscoveryEnabled.value = it }
            "advertising" -> (value as? Boolean)?.let { isAdvertisingEnabled.value = it }
            "haptic" -> (value as? Boolean)?.let { isHapticEnabled.value = it }
            "animation_speed" -> (value as? Float)?.let { animationSpeed.value = it }
            "haptic_intensity" -> (value as? Int)?.let { hapticIntensity.value = it }
            "message_preview" -> (value as? Boolean)?.let { messagePreview.value = it }
            "auto_read_receipts" -> (value as? Boolean)?.let { autoReadReceipts.value = it }
            "compact_mode" -> (value as? Boolean)?.let { compactMode.value = it }
            "show_timestamps" -> (value as? Boolean)?.let { showTimestamps.value = it }
            AppConfig.KEY_CONN_TIMEOUT -> (value as? Int)?.let { connectionTimeout.value = it }
            "max_image_size" -> (value as? Int)?.let { maxImageSize.value = it }
            "theme_mode" -> (value as? Int)?.let { themeMode.value = it }
            AppConfig.KEY_ENABLE_NEARBY -> (value as? Boolean)?.let { isNearbyEnabled.value = it }
            AppConfig.KEY_ENABLE_BLUETOOTH -> (value as? Boolean)?.let { isBluetoothEnabled.value = it }
            AppConfig.KEY_ENABLE_LAN -> (value as? Boolean)?.let { isLanEnabled.value = it }
            AppConfig.KEY_ENABLE_WIFI_DIRECT -> (value as? Boolean)?.let { isWifiDirectEnabled.value = it }
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
