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
import com.kai.ghostmesh.data.repository.GhostRepository
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
    private val repository = GhostRepository(database.messageDao(), database.profileDao())

    private val prefs = application.getSharedPreferences("chatex_prefs", Context.MODE_PRIVATE)
    private val myNodeId = prefs.getString("node_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("node_id", it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(id = myNodeId, name = "User_${android.os.Build.MODEL}"))
    val userProfile = _userProfile.asStateFlow()

    private val _onlineGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val onlineGhosts = _onlineGhosts.asStateFlow()

    val allKnownProfiles = repository.allProfiles.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChatHistory = _activeChatGhostId.flatMapLatest { ghostId ->
        if (ghostId != null) repository.getMessagesForGhost(ghostId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val isDiscoveryEnabled = MutableStateFlow(true)
    val isAdvertisingEnabled = MutableStateFlow(true)
    val isHapticEnabled = MutableStateFlow(true)
    val isEncryptionEnabled = MutableStateFlow(true)
    val selfDestructSeconds = MutableStateFlow(0)

    private var meshService: MeshService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            meshService = (service as MeshService.MeshBinder).getService()
            observeService()
        }
        override fun onServiceDisconnected(name: ComponentName?) { meshService = null }
    }

    init {
        val intent = Intent(application, MeshService::class.java).apply {
            putExtra("NICKNAME", _userProfile.value.name)
            putExtra("NODE_ID", myNodeId)
        }
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        viewModelScope.launch {
            while(true) {
                repository.burnExpired(System.currentTimeMillis())
                delay(2000)
            }
        }
    }

    private fun observeService() {
        meshService?.let { service ->
            viewModelScope.launch {
                service.incomingPackets.collect { packet -> handleIncomingPacket(packet) }
            }
            viewModelScope.launch {
                service.connectionUpdates.collect { ghosts ->
                    _onlineGhosts.value = ghosts.mapValues { entry -> 
                        val db = repository.getProfile(entry.key)
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
                repository.saveMessage(packet, isMe = false, isImage = packet.type == PacketType.IMAGE, expirySeconds = packet.expirySeconds)
                if (repository.getProfile(packet.senderId) == null) {
                    repository.syncProfile(ProfileEntity(packet.senderId, packet.senderName, "Discovered via Mesh"))
                }
            }
            PacketType.PROFILE_SYNC -> {
                val parts = packet.payload.split("|")
                if (parts.isNotEmpty()) {
                    val profile = ProfileEntity(packet.senderId, parts[0], parts.getOrNull(1) ?: "")
                    repository.syncProfile(profile)
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
            putExtra("NICKNAME", _userProfile.value.name); putExtra("NODE_ID", myNodeId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopMesh() = getApplication<Application>().stopService(Intent(getApplication(), MeshService::class.java))

    fun sendMessage(content: String) {
        val targetId = _activeChatGhostId.value ?: return
        val destruct = selfDestructSeconds.value > 0
        val packet = Packet(
            senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId,
            type = PacketType.CHAT, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content,
            isSelfDestruct = destruct, expirySeconds = selfDestructSeconds.value
        )
        meshService?.sendPacket(packet)
        viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, expirySeconds = selfDestructSeconds.value) }
    }

    fun sendImage(uri: Uri) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            val base64 = uriToBase64(uri) ?: return@launch
            val destruct = selfDestructSeconds.value > 0
            val packet = Packet(
                senderId = myNodeId, senderName = _userProfile.value.name, receiverId = targetId,
                type = PacketType.IMAGE, payload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64) else base64,
                isSelfDestruct = destruct, expirySeconds = selfDestructSeconds.value
            )
            meshService?.sendPacket(packet)
            repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = true, expirySeconds = selfDestructSeconds.value)
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

    fun clearHistory() = viewModelScope.launch { repository.purgeArchives() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
