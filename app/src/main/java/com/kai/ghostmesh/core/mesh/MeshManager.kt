package com.kai.ghostmesh.core.mesh

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.mesh.transports.GoogleNearbyTransport
import com.kai.ghostmesh.core.mesh.transports.BluetoothLegacyTransport
import com.kai.ghostmesh.core.mesh.transports.LanTransport
import com.kai.ghostmesh.core.mesh.transports.WifiDirectTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

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
    val connectionUpdates = MutableStateFlow<List<UserProfile>>(emptyList())

    val totalPacketsSent = MutableStateFlow(0)
    val totalPacketsReceived = MutableStateFlow(0)

    data class FileStatus(val fileName: String, val peerId: String, val progress: Float, val isComplete: Boolean = false, val error: String? = null)
    private val _fileTransferStatus = MutableSharedFlow<FileStatus>()
    val fileTransferStatus = _fileTransferStatus.asSharedFlow()

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        if (engine != null) return

        val callback = object : MeshTransport.Callback {
            override fun onPacketReceived(endpointId: String, json: String) {
                totalPacketsReceived.update { it + 1 }
                engine?.processIncomingJson(endpointId, json)
            }

            override fun onConnectionChanged(nodes: Map<String, String>) {
                val profiles = nodes.map { (id, name) ->
                    UserProfile(id = id.split(":").last(), name = name, isOnline = true)
                }
                connectionUpdates.value = profiles
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
                if (packet.type == PacketType.FILE) {
                    fileTransferManager?.receiveFilePacket(packet)
                } else {
                    scope.launch { incomingPackets.emit(packet) }
                }
            },
            onProfileUpdate = { id, name, status, battery, endpoint ->
                // Profile logic
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
    }

    fun sendPacket(packet: Packet) {
        engine?.sendPacket(packet)
    }

    fun sendHeartbeat() {
        engine?.generateHeartbeat()?.let { sendPacket(it) }
    }

    fun stop() {
        gatewayManager?.stop()
        transport?.stop()
        gatewayManager = null
        transport = null
        engine = null
        fileTransferManager = null
    }

    fun updateBattery(battery: Int) {
        engine?.updateMyBattery(battery)
        val intervalMs = when {
            battery < 10 -> 120000L // Critical power mode
            battery < 15 -> 60000L
            battery < 50 -> 30000L
            else -> 10000L
        }
        transport?.setScanInterval(intervalMs)
    }

    fun getRoutingTable() = engine?.getRoutingTable()

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
