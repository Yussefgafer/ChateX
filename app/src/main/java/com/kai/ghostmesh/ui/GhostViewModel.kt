package com.kai.ghostmesh.ui

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
    private val prefs = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val myNodeId = prefs.getString(Constants.KEY_NODE_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(Constants.KEY_NODE_ID, it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(
        id = myNodeId, 
        name = prefs.getString(Constants.KEY_NICKNAME, "User_${android.os.Build.MODEL.take(4)}")!!,
        status = prefs.getString(Constants.KEY_STATUS, "Roaming the void")!!,
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
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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
    private var mediaPlayer: MediaPlayer? = null

    init {
        val intent = Intent(application, MeshService::class.java).apply {
            putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId)
        }
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        viewModelScope.launch { while(true) { repository.burnExpired(System.currentTimeMillis()); checkMeshHealth(); delay(2000) } }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
        mediaRecorder?.release()
        mediaRecorder = null
        releaseMediaPlayer()
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
                    repository.syncProfile(ProfileEntity(packet.senderId, packet.senderName, "Mesh Discovery", color = getAvatarColor(packet.senderId)))
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
        if (content.isBlank()) return
        val targetId = _activeChatGhostId.value ?: "ALL"
        val destruct = selfDestructSeconds.value > 0
        val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.CHAT, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content, isSelfDestruct = destruct, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
        meshService?.sendPacket(packet)
        if (targetId != "ALL") {
            viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value) }
        }
    }

    fun globalShout(content: String) {
        if (content.isBlank()) return
        val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = "ALL", type = PacketType.CHAT, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content, hopCount = hopLimit.value)
        meshService?.sendPacket(packet)
        viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = 0, maxHops = hopLimit.value) }
    }

    fun deleteMessage(id: String) = viewModelScope.launch { repository.deleteMessage(id) }

    private var typingJob: kotlinx.coroutines.Job? = null
    fun sendTyping(isTyping: Boolean) {
        val targetId = _activeChatGhostId.value ?: return
        if (targetId == "ALL") return
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

    @Suppress("DEPRECATION")
    fun startRecording() {
        try {
            audioFile = File(getApplication<Application>().cacheDir, "spectral_voice.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(getApplication()) else MediaRecorder()
            mediaRecorder?.apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(audioFile?.absolutePath); prepare(); start() }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopRecording() {
        try { mediaRecorder?.apply { stop(); release() }; mediaRecorder = null; audioFile?.let { sendVoice(it) } } catch (e: Exception) { e.printStackTrace() }
    }

    private fun sendVoice(file: File) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            val base64 = fileToBase64(file) ?: return@launch
            val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.VOICE, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64) else base64, isSelfDestruct = selfDestructSeconds.value > 0, expirySeconds = selfDestructSeconds.value, hopCount = hopLimit.value)
            meshService?.sendPacket(packet)
            repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = false, isVoice = true, expirySeconds = selfDestructSeconds.value, maxHops = hopLimit.value)
        }
    }

    private fun fileToBase64(file: File): String? = try { Base64.encodeToString(file.readBytes(), Base64.DEFAULT) } catch (e: Exception) { null }

    fun playVoice(base64: String) {
        try {
            releaseMediaPlayer()
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tempFile = File(getApplication<Application>().cacheDir, "play_temp.m4a")
            FileOutputStream(tempFile).use { it.write(bytes) }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { 
                    release()
                    mediaPlayer = null
                    tempFile.delete()
                }
            }
        } catch (e: Exception) { 
            e.printStackTrace()
            releaseMediaPlayer()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        private const val MAX_IMAGE_DIMENSION = 1024
        private const val MAX_IMAGE_SIZE_BYTES = 500 * 1024
        private const val INITIAL_QUALITY = 80
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return null

            var sampleSize = 1
            while (originalWidth / sampleSize > MAX_IMAGE_DIMENSION * 2 ||
                   originalHeight / sampleSize > MAX_IMAGE_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeStream = getApplication<Application>().contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions) ?: return null
            decodeStream.close()

            val scaledBitmap = if (originalWidth > MAX_IMAGE_DIMENSION || originalHeight > MAX_IMAGE_DIMENSION) {
                val scale = minOf(
                    MAX_IMAGE_DIMENSION.toFloat() / originalWidth,
                    MAX_IMAGE_DIMENSION.toFloat() / originalHeight
                )
                val newWidth = (originalWidth * scale).toInt()
                val newHeight = (originalHeight * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            var quality = INITIAL_QUALITY
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            while (outputStream.size() > MAX_IMAGE_SIZE_BYTES && quality > 20) {
                quality -= 10
                outputStream.reset()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            val result = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            outputStream.close()
            scaledBitmap.recycle()
            result
        } catch (e: Exception) { 
            e.printStackTrace()
            null 
        }
    }

    private fun getAvatarColor(id: String): Int {
        val colors = listOf(0xFF00FF7F, 0xFFFF3131, 0xFFBB86FC, 0xFF00BFFF, 0xFFFFD700, 0xFFFF69B4)
        return colors[Math.abs(id.hashCode()) % colors.size].toInt()
    }

    fun startMesh() {
        if (isStealthMode.value) return
        val intent = Intent(getApplication(), MeshService::class.java).apply { putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId); putExtra("STEALTH", isStealthMode.value) }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) getApplication<Application>().startForegroundService(intent)
        else getApplication<Application>().startService(intent)
    }

    fun stopMesh() = getApplication<Application>().stopService(Intent(getApplication(), MeshService::class.java))
    fun clearHistory() = viewModelScope.launch { repository.purgeArchives() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }
    
    fun clearErrorMessage() { _errorMessage.value = null }
    
    fun showError(message: String) { _errorMessage.value = message }
    
    fun refreshConnections() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                stopMesh()
                kotlinx.coroutines.delay(500)
                startMesh()
            } catch (e: Exception) {
                showError("Failed to refresh connections")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
