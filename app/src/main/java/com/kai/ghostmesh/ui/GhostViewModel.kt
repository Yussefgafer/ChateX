package com.kai.ghostmesh.ui

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build // 🚀 Import fixed
import android.os.IBinder
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.data.repository.GhostRepository
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import com.kai.ghostmesh.service.MeshService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GhostViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = GhostRepository(database.messageDao(), database.profileDao())
    private val prefs = application.getSharedPreferences("chatex_prefs", Context.MODE_PRIVATE)

    private val myNodeId = prefs.getString("node_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("node_id", it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(
        id = myNodeId, 
        name = prefs.getString("nick", "User_${android.os.Build.MODEL.take(4)}")!!,
        status = prefs.getString("status", "Roaming the void")!!,
        color = prefs.getInt("soul_color", 0xFF00FF7F.toInt())
    ))
    val userProfile = _userProfile.asStateFlow()

    val spectralColor = _userProfile.map { Color(it.color) }.stateIn(viewModelScope, SharingStarted.Lazily, Color(0xFF00FF7F))

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

    // Settings
    val isDiscoveryEnabled = MutableStateFlow(prefs.getBoolean("discovery", true))
    val isAdvertisingEnabled = MutableStateFlow(prefs.getBoolean("advertising", true))
    val isStealthMode = MutableStateFlow(prefs.getBoolean("stealth", false))
    val isHapticEnabled = MutableStateFlow(prefs.getBoolean("haptic", true))
    val isEncryptionEnabled = MutableStateFlow(prefs.getBoolean("encryption", true))
    val selfDestructSeconds = MutableStateFlow(prefs.getInt("burn", 0))
    val hopLimit = MutableStateFlow(prefs.getInt("hops", 3))

    private val _packetsSent = MutableStateFlow(0)
    val packetsSent = _packetsSent.asStateFlow()
    private val _packetsReceived = MutableStateFlow(0)
    val packetsReceived = _packetsReceived.asStateFlow()
    private val _meshHealth = MutableStateFlow(100)
    val meshHealth = _meshHealth.asStateFlow()

    private var meshService: MeshService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            meshService = (service as MeshService.MeshBinder).getService()
            observeService()
        }
        override fun onServiceDisconnected(name: ComponentName?) { meshService = null }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    init {
        val intent = Intent(application, MeshService::class.java).apply {
            putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId)
        }
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        viewModelScope.launch { while(true) { repository.burnExpired(System.currentTimeMillis()); checkMeshHealth(); delay(2000) } }
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
                    _onlineGhosts.value = ghosts.mapValues { entry -> 
                        val db = repository.getProfile(entry.key)
                        UserProfile(id = entry.key, name = entry.value, status = db?.status ?: "Online", color = db?.color ?: getAvatarColor(entry.key))
                    }
                    syncProfile()
                }
            }
            viewModelScope.launch { service.totalPacketsSent.collect { _packetsSent.value = it } }
            viewModelScope.launch { service.totalPacketsReceived.collect { _packetsReceived.value = it } }
        }
    }

    private suspend fun handleIncomingPacket(packet: Packet) {
        when (packet.type) {
            PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE -> {
                repository.saveMessage(packet, isMe = false, isImage = packet.type == PacketType.IMAGE, isVoice = packet.type == PacketType.VOICE, expirySeconds = packet.expirySeconds, maxHops = hopLimit.value)
                if (repository.getProfile(packet.senderId) == null) {
                    repository.syncProfile(ProfileEntity(packet.senderId, packet.senderName, "Discovered"))
                }
                _typingGhosts.value -= packet.senderId
            }
            PacketType.ACK -> repository.updateMessageStatus(packet.payload, MessageStatus.DELIVERED)
            PacketType.TYPING_START -> _typingGhosts.value += packet.senderId
            PacketType.TYPING_STOP -> _typingGhosts.value -= packet.senderId
            PacketType.PROFILE_SYNC -> {
                val parts = packet.payload.split("|")
                if (parts.size >= 2) {
                    val incomingColor = parts.getOrNull(2)?.toIntOrNull() ?: getAvatarColor(packet.senderId)
                    repository.syncProfile(ProfileEntity(packet.senderId, parts[0], parts[1], color = incomingColor))
                }
            }
        }
    }

    fun updateMyProfile(name: String, status: String, colorHex: Int? = null) {
        val current = _userProfile.value
        val newProfile = current.copy(name = name, status = status, color = colorHex ?: current.color)
        _userProfile.value = newProfile
        prefs.edit().putString("nick", name).putString("status", status).putInt("soul_color", newProfile.color).apply()
        syncProfile()
    }

    fun updateSetting(key: String, value: Any) {
        prefs.edit().apply { when(value) { is Boolean -> putBoolean(key, value); is Int -> putInt(key, value) }; apply() }
        if (key == "stealth") meshService?.updateMeshConfig(isStealthMode.value, _userProfile.value.name)
    }

    private fun syncProfile() {
        if (isStealthMode.value) return
        val profile = _userProfile.value
        val payload = "${profile.name}|${profile.status}|${profile.color}"
        meshService?.sendPacket(Packet(senderId = myNodeId, senderName = profile.name, type = PacketType.PROFILE_SYNC, payload = payload))
    }

    fun sendMessage(content: String) {
        val targetId = _activeChatGhostId.value ?: "ALL"
        val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.CHAT, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content, isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
        meshService?.sendPacket(packet)
        if (targetId != "ALL") {
            viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value) }
        }
        sendTyping(false)
    }

    fun globalShout(content: String) {
        meshService?.sendPacket(Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = "ALL", type = PacketType.CHAT, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content, hopCount = hopLimit.value))
    }

    private var typingJob: kotlinx.coroutines.Job? = null
    fun sendTyping(isTyping: Boolean) {
        val targetId = _activeChatGhostId.value ?: return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            meshService?.sendPacket(Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = if (isTyping) PacketType.TYPING_START else PacketType.TYPING_STOP, payload = ""))
            if (isTyping) { delay(4000); sendTyping(false) }
        }
    }

    fun sendImage(uri: Uri) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            val base64 = uriToBase64(uri) ?: return@launch
            val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.IMAGE, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64) else base64, isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
            meshService?.sendPacket(packet)
            repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = true, isVoice = false, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value)
        }
    }

    fun startRecording() {
        try {
            audioFile = File(getApplication<Application>().cacheDir, "spectral_voice.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(getApplication()) else MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            audioFile?.let { sendVoice(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun sendVoice(file: File) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            val base64 = fileToBase64(file) ?: return@launch
            val encryptedPayload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64) else base64
            val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.VOICE, payload = encryptedPayload, isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
            meshService?.sendPacket(packet)
            repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = false, isVoice = true, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value)
        }
    }

    private fun fileToBase64(file: File): String? {
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) { null }
    }

    fun playVoice(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tempFile = File(getApplication<Application>().cacheDir, "play_temp.m4a")
            FileOutputStream(tempFile).use { it.write(bytes) }
            MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { it.release(); tempFile.delete() }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { null }
    }

    private fun getAvatarColor(id: String): Int {
        val colors = listOf(0xFF00FF7F, 0xFFFF3131, 0xFFBB86FC, 0xFF00BFFF, 0xFFFFD700, 0xFFFF69B4)
        return colors[Math.abs(id.hashCode()) % colors.size].toInt()
    }

    fun startMesh() {
        if (isStealthMode.value) return
        val intent = Intent(getApplication(), MeshService::class.java).apply { putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId) }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) getApplication<Application>().startForegroundService(intent)
        else getApplication<Application>().startService(intent)
    }

    fun stopMesh() = getApplication<Application>().stopService(Intent(getApplication(), MeshService::class.java))
    fun clearHistory() = viewModelScope.launch { repository.purgeArchives() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
