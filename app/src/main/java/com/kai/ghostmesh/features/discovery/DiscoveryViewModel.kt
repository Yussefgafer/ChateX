package com.kai.ghostmesh.features.discovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GhostApplication).container
    private val meshManager = container.meshManager
    private val repository = container.repository

    private val _connectedNodes = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val connectedNodes = _connectedNodes.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    val meshHealth = combine(_isConnected, _connectedNodes) { connected, nodes ->
        if (!connected) 0 else (nodes.size * 20).coerceAtMost(100)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            meshManager.connectionUpdates.collect { updates ->
                val nodes = updates.mapValues { (id, name) -> UserProfile(id = id, name = name, isOnline = true) }
                _connectedNodes.value = nodes
                _isConnected.value = updates.isNotEmpty()
            }
        }
    }

    fun globalShout(content: String, isEncryptionEnabled: Boolean, hopLimit: Int, myProfile: UserProfile) {
        if (content.isBlank()) return
        val encryptedPayload = if (isEncryptionEnabled) {
            SecurityManager.encrypt(content, null).getOrElse { viewModelScope.launch { _error.emit("Security error: Broadcast encryption failed") }; return }
        } else content

        val packetId = java.util.UUID.randomUUID().toString()
        val signature = SecurityManager.signPacket(packetId, encryptedPayload)

        val packet = Packet(
            id = packetId,
            senderId = container.myNodeId, senderName = myProfile.name, receiverId = "ALL",
            type = PacketType.CHAT, payload = encryptedPayload, hopCount = hopLimit,
            signature = signature
        )
        meshManager.sendPacket(packet)
        viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = 0, maxHops = hopLimit) }
    }
}
