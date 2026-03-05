package com.kai.ghostmesh.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.data.local.ProfileEntity
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.model.AppConfig
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.security.IdentityManager
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as? GhostApplication)?.container
        ?: (application.applicationContext as? GhostApplication)?.container

    private val repository: GhostRepository? = container?.repository
    private val prefs = application.getSharedPreferences("ghost_prefs", android.content.Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    // Preferences
    private val _isDiscoveryEnabled = MutableStateFlow(prefs.getBoolean("discovery", true))
    val isDiscoveryEnabled = _isDiscoveryEnabled.asStateFlow()

    private val _isAdvertisingEnabled = MutableStateFlow(prefs.getBoolean("advertising", true))
    val isAdvertisingEnabled = _isAdvertisingEnabled.asStateFlow()

    private val _isStealthMode = MutableStateFlow(prefs.getBoolean("stealth", false))
    val isStealthMode = _isStealthMode.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(prefs.getBoolean("haptic", true))
    val isHapticEnabled = _isHapticEnabled.asStateFlow()

    private val _isEncryptionEnabled = MutableStateFlow(prefs.getBoolean("encryption", true))
    val isEncryptionEnabled = _isEncryptionEnabled.asStateFlow()

    private val _selfDestructSeconds = MutableStateFlow(prefs.getInt("self_destruct", 0))
    val selfDestructSeconds = _selfDestructSeconds.asStateFlow()

    private val _hopLimit = MutableStateFlow(prefs.getInt(AppConfig.KEY_HOP_LIMIT, AppConfig.DEFAULT_HOP_LIMIT))
    val hopLimit = _hopLimit.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode = _themeMode.asStateFlow()

    private val _cornerRadius = MutableStateFlow(prefs.getInt(AppConfig.KEY_CORNER_RADIUS, AppConfig.DEFAULT_CORNER_RADIUS))
    val cornerRadius = _cornerRadius.asStateFlow()

    private val _fontScale = MutableStateFlow(prefs.getFloat(AppConfig.KEY_FONT_SCALE, AppConfig.DEFAULT_FONT_SCALE))
    val fontScale = _fontScale.asStateFlow()

    private val _isNearbyEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true))
    val isNearbyEnabled = _isNearbyEnabled.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true))
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private val _isLanEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true))
    val isLanEnabled = _isLanEnabled.asStateFlow()

    private val _isWifiDirectEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true))
    val isWifiDirectEnabled = _isWifiDirectEnabled.asStateFlow()

    // Data & Storage
    private val _autoDownloadImages = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_AUTO_DOWNLOAD_IMAGES, true))
    val autoDownloadImages = _autoDownloadImages.asStateFlow()

    private val _autoDownloadVideos = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_AUTO_DOWNLOAD_VIDEOS, false))
    val autoDownloadVideos = _autoDownloadVideos.asStateFlow()

    private val _autoDownloadFiles = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_AUTO_DOWNLOAD_FILES, false))
    val autoDownloadFiles = _autoDownloadFiles.asStateFlow()

    private val _downloadSizeLimit = MutableStateFlow(prefs.getInt(AppConfig.KEY_DOWNLOAD_SIZE_LIMIT, 5)) // MB
    val downloadSizeLimit = _downloadSizeLimit.asStateFlow()

    val packetsSent = container?.meshManager?.totalPacketsSent ?: MutableStateFlow(0)
    val packetsReceived = container?.meshManager?.totalPacketsReceived ?: MutableStateFlow(0)

    private val _mnemonic = MutableStateFlow<String?>(null)
    val mnemonic = _mnemonic.asStateFlow()

    // UI specific states
    val hapticIntensity = MutableStateFlow(0.8f)
    val animationSpeed = MutableStateFlow(1.0f)
    val messagePreview = MutableStateFlow(true)
    val autoReadReceipts = MutableStateFlow(true)
    val compactMode = MutableStateFlow(false)
    val showTimestamps = MutableStateFlow(true)
    val connectionTimeout = MutableStateFlow(30)
    val scanInterval = MutableStateFlow(10000)
    val maxImageSize = MutableStateFlow(2048)

    init {
        viewModelScope.launch {
            val profile = repository?.getProfile(container?.myNodeId ?: "")
            if (profile != null) {
                _userProfile.value = UserProfile(
                    id = profile.id,
                    name = profile.name,
                    status = profile.status,
                    color = profile.color
                )
            }
        }
    }

    fun updateMyProfile(name: String, status: String, color: Int?) {
        val current = _userProfile.value
        val updated = current.copy(name = name, status = status, color = color ?: current.color)
        _userProfile.value = updated
        viewModelScope.launch {
            repository?.syncProfile(ProfileEntity(
                id = updated.id,
                name = updated.name,
                status = updated.status,
                color = updated.color
            ))
        }
    }

    fun updateSetting(key: String, value: Any) {
        val editor = prefs.edit()
        when (key) {
            "discovery" -> { _isDiscoveryEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            "advertising" -> { _isAdvertisingEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            "stealth" -> { _isStealthMode.value = value as Boolean; editor.putBoolean(key, value) }
            "haptic" -> { _isHapticEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            "encryption" -> { _isEncryptionEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            "self_destruct" -> { _selfDestructSeconds.value = value as Int; editor.putInt(key, value) }
            AppConfig.KEY_HOP_LIMIT -> { _hopLimit.value = value as Int; editor.putInt(key, value) }
            "theme_mode" -> { _themeMode.value = value as String; editor.putString(key, value) }
            AppConfig.KEY_CORNER_RADIUS -> { _cornerRadius.value = value as Int; editor.putInt(key, value) }
            AppConfig.KEY_FONT_SCALE -> { _fontScale.value = value as Float; editor.putFloat(key, value) }
            AppConfig.KEY_ENABLE_NEARBY -> { _isNearbyEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_ENABLE_BLUETOOTH -> { _isBluetoothEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_ENABLE_LAN -> { _isLanEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_ENABLE_WIFI_DIRECT -> { _isWifiDirectEnabled.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_AUTO_DOWNLOAD_IMAGES -> { _autoDownloadImages.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_AUTO_DOWNLOAD_VIDEOS -> { _autoDownloadVideos.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_AUTO_DOWNLOAD_FILES -> { _autoDownloadFiles.value = value as Boolean; editor.putBoolean(key, value) }
            AppConfig.KEY_DOWNLOAD_SIZE_LIMIT -> { _downloadSizeLimit.value = value as Int; editor.putInt(key, value) }
        }
        editor.apply()
    }

    fun clearHistory() = viewModelScope.launch { repository?.purgeArchives() }

    fun generateBackupMnemonic() {
        val m = IdentityManager.generateMnemonic()
        _mnemonic.value = m
    }

    fun restoreIdentity(mnemonicString: String, onResult: (Boolean) -> Unit) {
        val success = SecurityManager.recoverIdentity(mnemonicString)
        onResult(success)
    }
}
