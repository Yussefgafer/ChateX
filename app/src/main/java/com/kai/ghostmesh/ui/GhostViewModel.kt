package com.kai.ghostmesh.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.mesh.MeshManager
import com.kai.ghostmesh.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GhostViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("chatex_prefs", Context.MODE_PRIVATE)
    private val myNodeId = prefs.getString("node_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("node_id", it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(id = myNodeId, name = "User_${android.os.Build.MODEL}"))
    val userProfile = _userProfile.asStateFlow()

    private val _connectedGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val connectedGhosts = _connectedGhosts.asStateFlow()

    private val _chatHistory = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val chatHistory = _chatHistory.asStateFlow()

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

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
                        updateHistory(packet.senderId, Message(packet.senderName, packet.payload, false))
                    }
                    PacketType.PROFILE_SYNC -> {
                        // Profile data is embedded in payload or inferred from packet
                        val parts = packet.payload.split("|")
                        if (parts.size >= 1) {
                            updateGhostProfile(packet.senderId, parts[0], parts.getOrNull(1) ?: "")
                        }
                    }
                    else -> {}
                }
            }
        },
        onConnectionChanged = { ghosts ->
            viewModelScope.launch {
                val current = _connectedGhosts.value.toMutableMap()
                val newMap = ghosts.mapValues { entry -> 
                    current[entry.key] ?: UserProfile(id = entry.key, name = entry.value)
                }
                _connectedGhosts.value = newMap
                syncProfile()
            }
        }
    )

    private fun updateGhostProfile(id: String, name: String, status: String) {
        val current = _connectedGhosts.value.toMutableMap()
        current[id] = UserProfile(id = id, name = name, status = status)
        _connectedGhosts.value = current
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
                updateHistory(targetId, Message("Me", content, true))
            }
        }
    }

    private fun updateHistory(ghostId: String, message: Message) {
        val currentHistory = _chatHistory.value.toMutableMap()
        val ghostMessages = currentHistory[ghostId]?.toMutableList() ?: mutableListOf()
        ghostMessages.add(message)
        currentHistory[ghostId] = ghostMessages
        _chatHistory.value = currentHistory
    }

    fun clearHistory() { _chatHistory.value = emptyMap() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }
}
