package com.kai.ghostmesh.core.mesh

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.core.data.local.ProfileEntity
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.mesh.transports.GoogleNearbyTransport
import com.kai.ghostmesh.core.mesh.transports.BluetoothLegacyTransport
import com.kai.ghostmesh.core.mesh.transports.LanTransport
import com.kai.ghostmesh.core.mesh.transports.WifiDirectTransport
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.math.exp
import java.io.File

class MeshManager(private val context: Context, private val myNodeId: String) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var engine: MeshEngine? = null
    private var transport: MultiTransportManager? = null
    private var gatewayManager: GatewayManager? = null
    var fileTransferManager: FileTransferManager? = null
        private set

    val incomingPackets = MutableSharedFlow<Packet>(
        extraBufferCapacity = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _rawConnections = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _routingTableVersion = MutableStateFlow(0)

    val totalPacketsSent = MutableStateFlow(0)
    val totalPacketsReceived = MutableStateFlow(0)

    data class FileStatus(val fileName: String, val peerId: String, val progress: Float, val isComplete: Boolean = false, val error: String? = null)
    private val _fileTransferStatus = MutableSharedFlow<FileStatus>()
    val fileTransferStatus = _fileTransferStatus.asSharedFlow()

    private var repository: GhostRepository? = null

    fun setRepository(repository: GhostRepository) {
        this.repository = repository
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val connectionUpdates: Flow<List<UserProfile>> = _routingTableVersion
        .debounce(500)
        .map {
            val currentEngine = engine
            val currentRepo = repository
            if (currentEngine != null && currentRepo != null) {
                val routingNodes = currentEngine.getRoutingTable()
                val profiles = mutableListOf<UserProfile>()
                routingNodes.forEach { (nodeId, route) ->
                    val profileEntity = currentRepo.getProfile(nodeId)
                    profiles.add(UserProfile(
                        id = nodeId,
                        name = profileEntity?.name ?: "Unknown Peer",
                        status = profileEntity?.status ?: "Active on network",
                        color = profileEntity?.color ?: 0xFF00FF7F.toInt(),
                        batteryLevel = route.battery,
                        isOnline = true,
                        bestEndpoint = route.nextHopEndpointId,
                        transportType = route.nextHopEndpointId.split(":").firstOrNull()
                    ))
                }
                profiles
            } else {
                emptyList()
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        if (engine != null) return

        val callback = object : MeshTransport.Callback {
            override fun onPacketReceived(endpointId: String, json: String) {
                totalPacketsReceived.update { it + 1 }
                engine?.processIncomingJson(endpointId, json)
            }

            override fun onConnectionChanged(nodes: Map<String, String>) {
                _rawConnections.value = nodes
                if (nodes.isNotEmpty()) {
                    announceIdentity(nickname)
                }
            }

            override fun onError(message: String) {}
        }

        transport = MultiTransportManager(callback)

        fileTransferManager = FileTransferManager(
            context = context,
            myNodeId = myNodeId,
            myNickname = nickname,
            sendPacket = { sendPacket(it) },
            onFileProgress = { fileName, peerId, progress ->
                scope.launch { _fileTransferStatus.emit(FileStatus(fileName, peerId, progress)) }
            },
            onFileComplete = { fileName, peerId, filePath ->
                scope.launch { _fileTransferStatus.emit(FileStatus(fileName, peerId, 1.0f, isComplete = true)) }
            },
            onFileError = { fileName, peerId, error ->
                scope.launch { _fileTransferStatus.emit(FileStatus(fileName, peerId, 0f, error = error)) }
            }
        )

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = nickname,
            onSendToNeighbors = { packet, except ->
                totalPacketsSent.update { it + 1 }
                transport?.sendPacket(packet, except)
            },
            onHandlePacket = { packet ->
                when (packet.type) {
                    PacketType.FILE -> fileTransferManager?.receiveFilePacket(packet)
                    PacketType.ACK -> { fileTransferManager?.receiveFilePacket(packet); scope.launch { incomingPackets.emit(packet) } }
                    PacketType.READ_RECEIPT -> {
                        scope.launch { repository?.markMessageRead(packet.payload) }
                    }
                    else -> scope.launch { incomingPackets.emit(packet) }
                }
                _routingTableVersion.update { it + 1 }
            },
            onProfileUpdate = { id, name, status, battery, endpoint ->
                scope.launch {
                    repository?.syncProfile(ProfileEntity(
                        id = id,
                        name = name,
                        status = status,
                        batteryLevel = battery,
                        bestEndpoint = endpoint,
                        isOnline = true
                    ))
                    _routingTableVersion.update { it + 1 }
                }
            }
        )

        gatewayManager = GatewayManager(
            context = context,
            myNodeId = myNodeId,
            myNickname = nickname,
            onBroadcastGateway = { packet -> sendPacket(packet) }
        )

        if (isGooglePlayServicesAvailable(context)) {
            transport?.registerTransport(GoogleNearbyTransport(name = "Nearby", context = context, myNodeId = myNodeId, callback = callback))
        }
        transport?.registerTransport(BluetoothLegacyTransport(name = "Bluetooth", context = context, myNodeId = myNodeId, callback = callback))
        transport?.registerTransport(LanTransport(name = "LAN", context = context, myNodeId = myNodeId, callback = callback))
        transport?.registerTransport(WifiDirectTransport(name = "WiFiDirect", context = context, myNodeId = myNodeId, callback = callback))

        transport?.start(nickname, isStealth)
        gatewayManager?.start(isStealth)

        announceIdentity(nickname)
    }

    private fun announceIdentity(nickname: String) {
        scope.launch {
            delay(2000)
            val profile = repository?.getProfile(myNodeId)
            val payload = "$nickname|${profile?.status ?: "Mesh"}|${profile?.color ?: 0}"
            val packetId = java.util.UUID.randomUUID().toString()
            val signature = SecurityManager.signPacket(packetId, payload)
            sendPacket(Packet(
                id = packetId,
                senderId = myNodeId,
                senderName = nickname,
                receiverId = "ALL",
                type = PacketType.PROFILE_SYNC,
                payload = payload,
                signature = signature
            ))
        }
    }

    fun sendPacket(packet: Packet) {
        engine?.sendPacket(packet)
    }

    fun sendHeartbeat() {
        engine?.generateHeartbeat()?.let { sendPacket(it) }
    }

    fun initiateFileTransfer(file: File, recipientId: String, mediaType: PacketType = PacketType.FILE) {
        fileTransferManager?.initiateFileTransfer(file, recipientId, mediaType)
    }

    fun sendReadReceipt(senderId: String, packetId: String, senderName: String) {
        val receiptId = java.util.UUID.randomUUID().toString()
        val signature = SecurityManager.signPacket(receiptId, packetId)
        sendPacket(Packet(
            id = receiptId,
            senderId = myNodeId,
            senderName = senderName,
            receiverId = senderId,
            type = PacketType.READ_RECEIPT,
            payload = packetId,
            signature = signature
        ))
    }

    fun stop() {
        gatewayManager?.stop()
        transport?.stop(); engine?.stop()
        gatewayManager = null
        transport = null
        engine = null
        fileTransferManager = null
    }

    fun updateBattery(battery: Int) {
        engine?.updateMyBattery(battery)

        val baseInterval = 10000.0 // Default 10s
        val batteryRatio = battery / 100.0

        val intervalMs = (baseInterval * exp(3.0 * (1.0 - batteryRatio))).toLong()
            .coerceIn(10000L, 300000L) // Bound between 10s and 5mins

        transport?.setScanInterval(intervalMs)
    }

    fun getRoutingTable() = engine?.getRoutingTable()

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
