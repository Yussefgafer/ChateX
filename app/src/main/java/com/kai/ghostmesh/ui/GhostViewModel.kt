package com.kai.ghostmesh.ui

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.data.repository.GhostRepository
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import com.kai.ghostmesh.service.MeshService
import com.kai.ghostmesh.util.AudioManager
import com.kai.ghostmesh.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class GhostViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = GhostRepository(database.messageDao(), database.profileDao())
    private val prefs = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager = AudioManager(application)

    private val myNodeId = prefs.getString(Constants.KEY_NODE_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(Constants.KEY_NODE_ID, it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(
        id = myNodeId, 
        name = prefs.getString(Constants.KEY_NICKNAME, "User_${Build.MODEL.take(4)}")!!,
        status = prefs.getString(Constants.KEY_STATUS, "Roaming the void")!!,
        color = prefs.getInt("soul_color", 0xFF00FF7F.toInt())
    ))
    val userProfile = _userProfile.asStateFlow()

    private val _onlineGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val onlineGhosts = _onlineGhosts.asStateFlow()
    
    private val _typingGhosts = MutableStateFlow<Set<String>>(emptySet())
    val typingGhosts = _typingGhosts.asStateFlow()

    val recentChats = repository.recentChats.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChatHistory = _activeChatGhostId.flatMapLatest { ghostId ->
        if (ghostId != null) repository.getMessagesForGhost(ghostId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI Configuration & States
    val isDiscoveryEnabled = MutableStateFlow(prefs.getBoolean("discovery", true))
    val isAdvertisingEnabled = MutableStateFlow(prefs.getBoolean("advertising", true))
    val isStealthMode = MutableStateFlow(prefs.getBoolean("stealth", false))
    val isHapticEnabled = MutableStateFlow(prefs.getBoolean("haptic", true))
    val isEncryptionEnabled = MutableStateFlow(prefs.getBoolean("encryption", true))
    val selfDestructSeconds = MutableStateFlow(prefs.getInt("burn", 0))
    val hopLimit = MutableStateFlow(prefs.getInt("hops", 3))

    val animationSpeed = MutableStateFlow(prefs.getFloat("animation_speed", 1.0f))
    val hapticIntensity = MutableStateFlow(prefs.getInt("haptic_intensity", 2))
    val messagePreview = MutableStateFlow(prefs.getBoolean("message_preview", true))
    val autoReadReceipts = MutableStateFlow(prefs.getBoolean("auto_read_receipts", true))
    val compactMode = MutableStateFlow(prefs.getBoolean("compact_mode", false))
    val showTimestamps = MutableStateFlow(prefs.getBoolean("show_timestamps", true))
    val connectionTimeout = MutableStateFlow(prefs.getInt(AppConfig.KEY_CONN_TIMEOUT, AppConfig.DEFAULT_CONNECTION_TIMEOUT_S))
    val scanInterval = MutableStateFlow(prefs.getLong(AppConfig.KEY_SCAN_INTERVAL, AppConfig.DEFAULT_SCAN_INTERVAL_MS))
    val maxImageSize = MutableStateFlow(prefs.getInt("max_image_size", 1048576))
    val themeMode = MutableStateFlow(prefs.getInt("theme_mode", 0))
    val packetCacheSize = MutableStateFlow(prefs.getInt("net_packet_cache", 2000))
    val cornerRadius = MutableStateFlow(prefs.getInt(AppConfig.KEY_CORNER_RADIUS, AppConfig.DEFAULT_CORNER_RADIUS))
    val fontScale = MutableStateFlow(prefs.getFloat(AppConfig.KEY_FONT_SCALE, AppConfig.DEFAULT_FONT_SCALE))

    val isNearbyEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true))
    val isBluetoothEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true))
    val isLanEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true))
    val isWifiDirectEnabled = MutableStateFlow(prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true))

    private val _packetsSent = MutableStateFlow(0)
    val packetsSent = _packetsSent.asStateFlow()
    private val _packetsReceived = MutableStateFlow(0)
    val packetsReceived = _packetsReceived.asStateFlow()
    private val _meshHealth = MutableStateFlow(100)
    val meshHealth = _meshHealth.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _replyToMessage = MutableStateFlow<ReplyInfo?>(null)
    val replyToMessage = _replyToMessage.asStateFlow()

    private val _pendingMessages = MutableStateFlow<List<Packet>>(emptyList())
    private val _blockedContacts = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingAcks = MutableStateFlow<Map<String, Packet>>(emptyMap())

    data class ReplyInfo(val messageId: String, val messageContent: String, val senderName: String)

    private var meshService: MeshService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            meshService = (service as MeshService.MeshBinder).getService()
            observeService()
        }
        override fun onServiceDisconnected(name: ComponentName?) { meshService = null }
    }

    init {
        _blockedContacts.value = prefs.getStringSet("blocked_contacts", emptySet()) ?: emptySet()
        val intent = Intent(application, MeshService::class.java).apply {
            putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId)
        }
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        viewModelScope.launch { 
            while(true) { 
                repository.burnExpired(System.currentTimeMillis())
                checkMeshHealth()
                checkUnackedMessages()
                delay(5000) 
            } 
        }
    }

    private fun checkUnackedMessages() {
        if (!_isConnected.value) return
        val now = System.currentTimeMillis()
        _pendingAcks.value.filter { (_, packet) -> now - packet.timestamp > 10000 }.forEach { (_, packet) ->
            meshService?.sendPacket(packet)
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
        audioManager.release()
    }

    private fun checkMeshHealth() {
        val bm = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        _meshHealth.value = if (bm.adapter?.isEnabled == true) 100 else 0
    }

    private fun observeService() {
        meshService?.let { service ->
            viewModelScope.launch { service.incomingPackets.collect { handleIncomingPacket(it) } }
            viewModelScope.launch {
                service.connectionUpdates.collect { ghosts ->
                    val wasConnected = _isConnected.value
                    _onlineGhosts.value = ghosts.mapValues { entry -> 
                        val db = repository.getProfile(entry.key)
                        UserProfile(id = entry.key, name = entry.value, status = db?.status ?: "Online", color = db?.color ?: getAvatarColor(entry.key))
                    }
                    _isConnected.value = ghosts.isNotEmpty()
                    if (!wasConnected && _isConnected.value && _pendingMessages.value.isNotEmpty()) retryPendingMessages()
                    syncProfile()
                }
            }
            viewModelScope.launch { service.totalPacketsSent.collect { _packetsSent.value = it } }
            viewModelScope.launch { service.totalPacketsReceived.collect { _packetsReceived.value = it } }
        }
    }

    private suspend fun handleIncomingPacket(packet: Packet) {
        if (_blockedContacts.value.contains(packet.senderId)) return
        when (packet.type) {
            PacketType.KEY_EXCHANGE -> {
                SecurityManager.establishSession(packet.senderId, packet.payload)
                if (!isStealthMode.value) SecurityManager.getMyPublicKey()?.let {
                    meshService?.sendPacket(Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = packet.senderId, type = PacketType.KEY_EXCHANGE, payload = it))
                }
            }
            PacketType.ACK -> {
                _pendingAcks.value = _pendingAcks.value - packet.payload
                repository.updateMessageStatus(packet.payload, MessageStatus.DELIVERED)
            }
            PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE -> {
                repository.saveMessage(packet, isMe = false, isImage = packet.type == PacketType.IMAGE, isVoice = packet.type == PacketType.VOICE, expirySeconds = packet.expirySeconds, maxHops = hopLimit.value)
                if (isEncryptionEnabled.value && packet.receiverId != "ALL") SecurityManager.getMyPublicKey()?.let {
                    meshService?.sendPacket(Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = packet.senderId, type = PacketType.KEY_EXCHANGE, payload = it))
                }
                if (repository.getProfile(packet.senderId) == null) repository.syncProfile(ProfileEntity(packet.senderId, packet.senderName, "Mesh Discovery", color = getAvatarColor(packet.senderId)))
                _typingGhosts.value -= packet.senderId
            }
            PacketType.TYPING_START -> _typingGhosts.value += packet.senderId
            PacketType.TYPING_STOP -> _typingGhosts.value -= packet.senderId
            PacketType.PROFILE_SYNC -> packet.payload.split("|").let { parts ->
                if (parts.size >= 2) repository.syncProfile(ProfileEntity(packet.senderId, parts[0], parts[1], color = parts.getOrNull(2)?.toIntOrNull() ?: getAvatarColor(packet.senderId)))
            }
            PacketType.LAST_SEEN -> repository.updateLastSeen(packet.senderId, true)
            PacketType.PROFILE_IMAGE -> repository.updateProfileImage(packet.senderId, packet.payload)
            else -> {}
        }
    }

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
            meshService?.updateMeshConfig(isStealthMode.value, _userProfile.value.name, myNodeId)
        }
    }

    private fun syncProfile() {
        if (isStealthMode.value) return
        val profile = _userProfile.value
        meshService?.sendPacket(Packet(senderId = myNodeId, senderName = profile.name, type = PacketType.PROFILE_SYNC, payload = "${profile.name}|${profile.status}|${profile.color}"))
        SecurityManager.getMyPublicKey()?.let { pubKey ->
            meshService?.sendPacket(Packet(senderId = myNodeId, senderName = profile.name, type = PacketType.KEY_EXCHANGE, payload = pubKey))
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val targetId = _activeChatGhostId.value ?: "ALL"
        val replyInfo = _replyToMessage.value
        val packet = Packet(
            senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.CHAT,
            payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content, if(targetId == "ALL") null else targetId) else content,
            isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value,
            replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName
        )
        if (targetId != "ALL") _pendingAcks.value = _pendingAcks.value + (packet.id to packet)
        if (_isConnected.value) meshService?.sendPacket(packet) else {
            _pendingMessages.value = _pendingMessages.value + packet
            showError("Message queued - will send when connected")
        }
        if (targetId != "ALL") viewModelScope.launch {
            repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value, replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName)
        }
        _replyToMessage.value = null
    }

    fun setReplyTo(messageId: String, content: String, sender: String) { _replyToMessage.value = ReplyInfo(messageId, content, sender) }
    fun clearReply() { _replyToMessage.value = null }
    fun setActiveChat(id: String?) { _activeChatGhostId.value = id }
    fun clearErrorMessage() { _errorMessage.value = null }
    fun showError(message: String) { _errorMessage.value = message }
    fun deleteMessage(id: String) = viewModelScope.launch { repository.deleteMessage(id) }
    fun clearHistory() = viewModelScope.launch { repository.purgeArchives() }

    fun globalShout(content: String) {
        if (content.isBlank()) return
        val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = "ALL", type = PacketType.CHAT, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content, null) else content, hopCount = hopLimit.value)
        if (_isConnected.value) meshService?.sendPacket(packet) else _pendingMessages.value = _pendingMessages.value + packet
        viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = 0, maxHops = hopLimit.value) }
    }

    private suspend fun retryPendingMessages() {
        _pendingMessages.value.forEach { meshService?.sendPacket(it); delay(100) }
        _pendingMessages.value = emptyList()
    }

    fun sendTyping(isTyping: Boolean) {
        val targetId = _activeChatGhostId.value ?: return
        if (targetId == "ALL") return
        viewModelScope.launch {
            meshService?.sendPacket(Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = if (isTyping) PacketType.TYPING_START else PacketType.TYPING_STOP, payload = ""))
        }
    }

    fun sendImage(uri: Uri) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            ImageUtils.uriToBase64(getApplication(), uri, 2 * 1024 * 1024)?.let { base64 ->
                val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.IMAGE, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64, targetId) else base64, isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
                meshService?.sendPacket(packet)
                repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = true, isVoice = false, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value)
            } ?: showError("Image processing failed")
        }
    }

    fun startRecording() = audioManager.startRecording()
    fun stopRecording() = audioManager.stopRecording()?.let { file ->
        val targetId = _activeChatGhostId.value ?: return@let
        viewModelScope.launch {
            Base64.encodeToString(file.readBytes(), Base64.DEFAULT)?.let { base64 ->
                val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.VOICE, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64, targetId) else base64, isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
                meshService?.sendPacket(packet)
                repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = false, isVoice = true, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value)
            }
        }
    }
    fun playVoice(base64: String) = audioManager.playAudio(base64)

    private fun getAvatarColor(id: String): Int = listOf(0xFFD0BCFF, 0xFF80DEEA, 0xFFA5D6A7, 0xFFFFF59D, 0xFFFFAB91, 0xFFF48FB1).let { colors ->
        colors[Math.abs(id.hashCode()) % colors.size].toInt()
    }

    fun startMesh() {
        if (isStealthMode.value) return
        val intent = Intent(getApplication(), MeshService::class.java).apply { putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId); putExtra("STEALTH", isStealthMode.value) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getApplication<Application>().startForegroundService(intent) else getApplication<Application>().startService(intent)
    }

    fun stopMesh() = getApplication<Application>().stopService(Intent(getApplication(), MeshService::class.java))
    
    fun refreshConnections() = viewModelScope.launch {
        _isRefreshing.value = true
        stopMesh(); delay(500); startMesh()
        _isRefreshing.value = false
    }
}
