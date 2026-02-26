package com.kai.ghostmesh.ui

import android.app.Application
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.IBinder
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import com.kai.ghostmesh.service.MeshService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class GhostViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val messageDao = database.messageDao()
    private val profileDao = database.profileDao()

    private val prefs = application.getSharedPreferences("chatex_prefs", Context.MODE_PRIVATE)
    private val myNodeId = prefs.getString("node_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("node_id", it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(id = myNodeId, name = "User_${android.os.Build.MODEL}"))
    val userProfile = _userProfile.asStateFlow()

    private val _onlineGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val onlineGhosts = _onlineGhosts.asStateFlow()

    val allKnownProfiles = profileDao.getAllProfiles().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChatHistory = _activeChatGhostId.flatMapLatest { ghostId ->
        if (ghostId != null) {
            messageDao.getMessagesForGhost(ghostId).map { entities ->
                entities.map { Message(it.senderName, it.content, it.isMe, it.isImage, it.isSelfDestruct, it.expiryTime, it.timestamp) }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val isDiscoveryEnabled = MutableStateFlow(true)
    val isAdvertisingEnabled = MutableStateFlow(true)
    val isHapticEnabled = MutableStateFlow(true)
    val isEncryptionEnabled = MutableStateFlow(true)
    val selfDestructSeconds = MutableStateFlow(0)

    // Service Connection
    private var meshService: MeshService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeshService.MeshBinder
            meshService = binder.getService()
            observeService()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            meshService = null
        }
    }

    init {
        val intent = Intent(application, MeshService::class.java).apply {
            putExtra("NICKNAME", _userProfile.value.name)
            putExtra("NODE_ID", myNodeId)
        }
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        // Background Burner
        viewModelScope.launch {
            while(true) {
                messageDao.deleteExpiredMessages(System.currentTimeMillis())
                delay(2000)
            }
        }
    }

    private fun observeService() {
        meshService?.let { service ->
            viewModelScope.launch {
                service.incomingPackets.collect { packet ->
                    handleIncomingPacket(packet)
                }
            }
            viewModelScope.launch {
                service.connectionUpdates.collect { ghosts ->
                    _onlineGhosts.value = ghosts.mapValues { entry -> 
                        val db = profileDao.getProfileById(entry.key)
                        UserProfile(id = entry.key, name = entry.value, status = db?.status ?: "Online")
                    }
                    syncProfile()
                }
            }
        }
    }

    private suspend fun handleIncomingPacket(packet: Packet) {
        when (packet.type) {
            PacketType.CHAT, PacketType.IMAGE -> {
                val decryptedPayload = SecurityManager.decrypt(packet.payload)
                val expiryTime = if (packet.isSelfDestruct) System.currentTimeMillis() + (packet.expirySeconds * 1000) else 0
                messageDao.insertMessage(MessageEntity(
                    ghostId = packet.senderId, senderName = packet.senderName, content = decryptedPayload,
                    isMe = false, isImage = packet.type == PacketType.IMAGE,
                    isSelfDestruct = packet.isSelfDestruct, expiryTime = expiryTime, timestamp = packet.timestamp
                ))
            }
            PacketType.PROFILE_SYNC -> {
                val parts = packet.payload.split("|")
                if (parts.isNotEmpty()) {
                    profileDao.insertProfile(ProfileEntity(packet.senderId, parts[0], parts.getOrNull(1) ?: ""))
                }
            }
            else -> {}
        }
    }

    fun updateMyProfile(name: String, status: String) {
        _userProfile.value = _userProfile.value.copy(name = name, status = status)
        syncProfile()
    }

    private fun syncProfile() {
        val profile = _userProfile.value
        val packet = Packet(senderId = myNodeId, senderName = profile.name, type = PacketType.PROFILE_SYNC, payload = "${profile.name}|${profile.status}")
        meshService?.sendPacket(packet)
    }

    fun startMesh() {
        val intent = Intent(getApplication(), MeshService::class.java).apply {
            putExtra("NICKNAME", _userProfile.value.name)
            putExtra("NODE_ID", myNodeId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopMesh() {
        getApplication<Application>().stopService(Intent(getApplication(), MeshService::class.java))
    }

    fun sendMessage(content: String) {
        val targetId = _activeChatGhostId.value ?: return
        val destruct = selfDestructSeconds.value > 0
        val encryptedPayload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content
        val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.CHAT, payload = encryptedPayload, isSelfDestruct = destruct, expirySeconds = selfDestructSeconds.value)
        meshService?.sendPacket(packet)
        
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(ghostId = targetId, senderName = "Me", content = content, isMe = true, isImage = false, isSelfDestruct = destruct, expiryTime = if (destruct) System.currentTimeMillis() + (selfDestructSeconds.value * 1000) else 0, timestamp = System.currentTimeMillis()))
        }
    }

    fun sendImage(uri: Uri) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            val base64 = uriToBase64(uri) ?: return@launch
            val destruct = selfDestructSeconds.value > 0
            val encryptedPayload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64) else base64
            val packet = Packet(senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId, type = PacketType.IMAGE, payload = encryptedPayload, isSelfDestruct = destruct, expirySeconds = selfDestructSeconds.value)
            meshService?.sendPacket(packet)
            messageDao.insertMessage(MessageEntity(ghostId = targetId, senderName = "Me", content = base64, isMe = true, isImage = true, isSelfDestruct = destruct, expiryTime = if (destruct) System.currentTimeMillis() + (selfDestructSeconds.value * 1000) else 0, timestamp = System.currentTimeMillis()))
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { null }
    }

    fun clearHistory() = viewModelScope.launch { messageDao.clearAllMessages() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
