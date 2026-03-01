package com.kai.ghostmesh.features.discovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GhostApplication).container
    private val meshManager = container.meshManager
    private val repository = container.repository

    val connectedNodes = meshManager.knownNodes.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    val routingTable = MutableStateFlow<Map<String, com.kai.ghostmesh.core.mesh.Route>>(emptyMap())
    private val _neighborLocations = MutableStateFlow<Map<String, GeoPoint>>(emptyMap())
    val neighborLocations = _neighborLocations.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    val meshHealth = combine(isConnected, connectedNodes) { connected, nodes ->
        if (!connected && nodes.isEmpty()) 0
        else (nodes.filter { it.value.isOnline }.size * 20).coerceAtMost(100)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            meshManager.connectionUpdates.collect { updates ->
                _isConnected.value = updates.isNotEmpty()
                routingTable.value = meshManager.getRoutingTable() ?: emptyMap()
            }
        }
        viewModelScope.launch {
            meshManager.incomingPackets.collect { packet ->
                if (packet.type == PacketType.LOCATION_UPDATE) handleLocationUpdate(packet)
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                routingTable.value = meshManager.getRoutingTable() ?: emptyMap()
            }
        }
    }

    private fun handleLocationUpdate(packet: Packet) {
        try {
            val parts = packet.payload.split("|")
            val lat = parts[0].toDouble(); val lon = parts[1].toDouble()
            _neighborLocations.update { it + (packet.senderId to GeoPoint(lat, lon)) }
        } catch (e: Exception) {}
    }

    fun globalShout(content: String, isEncryptionEnabled: Boolean, hopLimit: Int, myProfile: UserProfile) {
        if (content.isBlank()) return
        val packet = Packet(senderId = container.myNodeId, senderName = myProfile.name, receiverId = "ALL", type = PacketType.CHAT, payload = if (isEncryptionEnabled) SecurityManager.encrypt(content, null) else content, hopCount = hopLimit)
        meshManager.sendPacket(packet)
        viewModelScope.launch { repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = 0, maxHops = hopLimit) }
    }
}
