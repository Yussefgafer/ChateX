package com.kai.ghostmesh.features.discovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.R
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val container = (application as? GhostApplication)?.container 
        ?: (application.applicationContext as? GhostApplication)?.container

    private val meshManager: MeshManager? = container?.meshManager
    private val repository: GhostRepository? = container?.repository

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
            meshManager?.connectionUpdates?.collect { updates ->
                val nodesMap = updates.associateBy { it.id }
                _connectedNodes.value = nodesMap
                _isConnected.value = updates.isNotEmpty()
            }
        }
    }

    fun globalShout(content: String, isEncryptionEnabled: Boolean, hopLimit: Int, myProfile: UserProfile) {
        if (content.isBlank() || container == null || meshManager == null) return
        viewModelScope.launch {
            try {
                var actualEncrypted = false
                val payloadToSend = if (isEncryptionEnabled) {
                    val res = SecurityManager.encrypt(content, null)
                    if (res.isSuccess) {
                        actualEncrypted = true
                        res.getOrThrow()
                    } else {
                        _error.emit("Global encryption failed, sending as plain text...")
                        content
                    }
                } else content

                val packetId = java.util.UUID.randomUUID().toString()
                val signature = SecurityManager.signPacket(packetId, payloadToSend)

                val packet = Packet(
                    id = packetId,
                    senderId = container.myNodeId, senderName = myProfile.name, receiverId = "ALL",
                    type = PacketType.CHAT, payload = payloadToSend, hopCount = hopLimit,
                    signature = signature,
                    isEncrypted = actualEncrypted
                )
                meshManager.sendPacket(packet)
                repository?.saveMessage(packet.copy(payload = content, isEncrypted = actualEncrypted), isMe = true, isImage = false, isVoice = false, isVideo = false, expirySeconds = 0, maxHops = hopLimit)
            } catch (e: Exception) {
                _error.emit(getApplication<Application>().getString(R.string.error_broadcast_failed, e.message))
            }
        }
    }
}
