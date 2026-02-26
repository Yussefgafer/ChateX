package com.kai.ghostmesh.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.mesh.MeshManager
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
                entities.map { Message(it.senderName, it.content, it.isMe, it.isSelfDestruct, it.expiryTime, it.timestamp) }
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
    val selfDestructSeconds = MutableStateFlow(0) // 0 = off

    private val meshManager = MeshManager(
        context = application,
        myNodeId = myNodeId,
        onPacketReceived = { packet ->
            viewModelScope.launch {
                when (packet.type) {
                    PacketType.CHAT -> {
                        val decryptedPayload = SecurityManager.decrypt(packet.payload)
                        val expiryTime = if (packet.isSelfDestruct) System.currentTimeMillis() + (packet.expirySeconds * 1000) else 0
                        
                        val entity = MessageEntity(
                            ghostId = packet.senderId,
                            senderName = packet.senderName,
                            content = decryptedPayload,
                            isMe = false,
                            isSelfDestruct = packet.isSelfDestruct,
                            expiryTime = expiryTime,
                            timestamp = packet.timestamp
                        )
                        messageDao.insertMessage(entity)
                    }
                    PacketType.PROFILE_SYNC -> {
                        val parts = packet.payload.split("|")
                        if (parts.isNotEmpty()) {
                            profileDao.insertProfile(ProfileEntity(packet.senderId, parts[0], parts.getOrNull(1) ?: ""))
                            updateOnlineGhost(packet.senderId, parts[0], parts.getOrNull(1) ?: "")
                        }
                    }
                    else -> {}
                }
            }
        },
        onConnectionChanged = { ghosts ->
            viewModelScope.launch {
                _onlineGhosts.value = ghosts.mapValues { entry -> 
                    val db = profileDao.getProfileById(entry.key)
                    UserProfile(id = entry.key, name = entry.value, status = db?.status ?: "Online")
                }
                syncProfile()
            }
        }
    )

    // 🚀 Background Burner: Cleanup expired messages every 2 seconds
    init {
        viewModelScope.launch {
            while(true) {
                messageDao.deleteExpiredMessages(System.currentTimeMillis())
                delay(2000)
            }
        }
    }

    private fun updateOnlineGhost(id: String, name: String, status: String) {
        val current = _onlineGhosts.value.toMutableMap()
        current[id] = UserProfile(id = id, name = name, status = status)
        _onlineGhosts.value = current
    }

    fun updateMyProfile(name: String, status: String) {
        _userProfile.value = _userProfile.value.copy(name = name, status = status)
        syncProfile()
    }

    private fun syncProfile() {
        val profile = _userProfile.value
        meshManager.sendPacket(Packet(senderId = myNodeId, senderName = profile.name, type = PacketType.PROFILE_SYNC, payload = "${profile.name}|${profile.status}"))
    }

    fun startMesh() = meshManager.startMesh(_userProfile.value.name)
    fun stopMesh() = meshManager.stop()

    fun sendMessage(content: String) {
        val targetId = _activeChatGhostId.value ?: "ALL"
        val destruct = selfDestructSeconds.value > 0
        
        val encryptedPayload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content
        val packet = Packet(
            senderId = myNodeId,
            senderName = _userProfile.value.name,
            receiverId = targetId,
            type = PacketType.CHAT,
            payload = encryptedPayload,
            isSelfDestruct = destruct,
            expirySeconds = selfDestructSeconds.value
        )
        meshManager.sendPacket(packet)
        
        if (targetId != "ALL") {
            viewModelScope.launch {
                messageDao.insertMessage(MessageEntity(
                    ghostId = targetId,
                    senderName = "Me",
                    content = content,
                    isMe = true,
                    isSelfDestruct = destruct,
                    expiryTime = if (destruct) System.currentTimeMillis() + (selfDestructSeconds.value * 1000) else 0,
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    fun clearHistory() = viewModelScope.launch { messageDao.clearAllMessages() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }
}
