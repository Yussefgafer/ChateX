package com.kai.ghostmesh.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.mesh.MeshManager
import com.kai.ghostmesh.model.*
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

    // Real-time connected ghosts
    private val _onlineGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val onlineGhosts = _onlineGhosts.asStateFlow()

    // All known profiles from DB
    val allKnownProfiles = profileDao.getAllProfiles().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    val activeChatHistory = _activeChatGhostId.flatMapLatest { ghostId ->
        if (ghostId != null) {
            messageDao.getMessagesForGhost(ghostId).map { entities ->
                entities.map { Message(it.senderName, it.content, it.isMe, it.timestamp) }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val isDiscoveryEnabled = MutableStateFlow(true)
    val isAdvertisingEnabled = MutableStateFlow(true)
    val isHapticEnabled = MutableStateFlow(true)

    private val meshManager = MeshManager(
        context = application,
        myNodeId = myNodeId,
        onPacketReceived = { packet ->
            viewModelScope.launch {
                when (packet.type) {
                    PacketType.CHAT -> {
                        val entity = MessageEntity(
                            ghostId = packet.senderId,
                            senderName = packet.senderName,
                            content = packet.payload,
                            isMe = false,
                            timestamp = packet.timestamp
                        )
                        messageDao.insertMessage(entity)
                        // Ensure we have a profile for this sender
                        if (profileDao.getProfileById(packet.senderId) == null) {
                            profileDao.insertProfile(ProfileEntity(packet.senderId, packet.senderName, "New ghost discovered"))
                        }
                    }
                    PacketType.PROFILE_SYNC -> {
                        val parts = packet.payload.split("|")
                        if (parts.isNotEmpty()) {
                            val profile = ProfileEntity(
                                id = packet.senderId,
                                name = parts[0],
                                status = parts.getOrNull(1) ?: "",
                                lastSeen = System.currentTimeMillis()
                            )
                            profileDao.insertProfile(profile)
                            updateOnlineGhost(packet.senderId, parts[0], parts.getOrNull(1) ?: "")
                        }
                    }
                    else -> {}
                }
            }
        },
        onConnectionChanged = { ghosts ->
            viewModelScope.launch {
                val currentOnline = _onlineGhosts.value.toMutableMap()
                // Update online status
                val newOnline = ghosts.mapValues { entry -> 
                    val dbProfile = profileDao.getProfileById(entry.key)
                    UserProfile(
                        id = entry.key, 
                        name = entry.value, 
                        status = dbProfile?.status ?: "Online"
                    )
                }
                _onlineGhosts.value = newOnline
                syncProfile()
            }
        }
    )

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
        val packet = Packet(
            senderId = myNodeId,
            senderName = profile.name,
            type = PacketType.PROFILE_SYNC,
            payload = "${profile.name}|${profile.status}"
        )
        meshManager.sendPacket(packet)
    }

    fun startMesh() = meshManager.startMesh(_userProfile.value.name)
    fun stopMesh() = meshManager.stop()

    fun sendMessage(content: String) {
        val targetId = _activeChatGhostId.value ?: "ALL"
        val packet = Packet(
            senderId = myNodeId,
            senderName = _userProfile.value.name,
            receiverId = targetId,
            type = PacketType.CHAT,
            payload = content
        )
        meshManager.sendPacket(packet)
        
        viewModelScope.launch {
            if (targetId != "ALL") {
                messageDao.insertMessage(MessageEntity(
                    ghostId = targetId,
                    senderName = "Me",
                    content = content,
                    isMe = true,
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    fun clearHistory() = viewModelScope.launch { 
        messageDao.clearAllMessages() 
    }
    
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }
}
