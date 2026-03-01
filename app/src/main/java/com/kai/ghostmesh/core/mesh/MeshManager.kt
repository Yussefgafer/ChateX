package com.kai.ghostmesh.core.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.core.mesh.transports.*
import com.kai.ghostmesh.core.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class MeshManager(
    private val context: Context,
    private val myNodeId: String
) {

    private var transport: MultiTransportManager? = null
    private var engine: MeshEngine? = null
    private var gatewayManager: GatewayManager? = null
    private var fileTransferManager: FileTransferManager? = null

    private val _incomingPackets = MutableSharedFlow<Packet>()
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _connectionUpdates = MutableSharedFlow<Map<String, String>>()
    val connectionUpdates = _connectionUpdates.asSharedFlow()

    private val _knownNodes = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val knownNodes = _knownNodes.asStateFlow()

    private val _totalPacketsSent = MutableStateFlow(0)
    val totalPacketsSent = _totalPacketsSent.asStateFlow()

    private val _totalPacketsReceived = MutableStateFlow(0)
    val totalPacketsReceived = _totalPacketsReceived.asStateFlow()

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val transportCallback = object : MeshTransport.Callback {
            override fun onPacketReceived(endpointId: String, json: String) {
                engine?.processIncomingJson(endpointId, json)
            }

            override fun onBinaryPacketReceived(endpointId: String, data: ByteArray) {
                engine?.processIncomingBinary(endpointId, data)
            }

            override fun onConnectionChanged(nodes: Map<String, String>) {
                _connectionUpdates.tryEmit(nodes)
                updatePresenceFromConnections(nodes)
            }

            override fun onError(message: String) {
                Log.e("MeshManager", "Transport Error: $message")
            }
        }

        transport = MultiTransportManager(transportCallback)

        if (prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true) && isGooglePlayServicesAvailable(context)) {
            transport?.registerTransport(GoogleNearbyTransport(context = context, myNodeId = myNodeId, callback = transportCallback))
        }
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true)) {
            transport?.registerTransport(BluetoothLegacyTransport(context = context, myNodeId = myNodeId, callback = transportCallback))
        }
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true)) {
            transport?.registerTransport(LanTransport(context = context, myNodeId = myNodeId, callback = transportCallback))
        }
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true)) {
            transport?.registerTransport(WifiDirectTransport(context = context, myNodeId = myNodeId, callback = transportCallback))
        }

        val cloudTransport = CloudTransport().apply { setNodeId(myNodeId) }
        transport?.registerTransport(cloudTransport)

        fileTransferManager = FileTransferManager(context, myNodeId) { packet ->
            engine?.sendPacket(packet)
        }

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = nickname,
            cacheSize = prefs.getInt("net_packet_cache", 2000),
            onSendToNeighbors = { packet, exceptId -> transport?.sendPacket(packet, exceptId) },
            onHandlePacket = { packet ->
                _totalPacketsReceived.value++
                when (packet.type) {
                    PacketType.BITFIELD, PacketType.CHUNK_REQUEST, PacketType.CHUNK_RESPONSE -> {
                        fileTransferManager?.handlePacket(packet)
                    }
                    else -> _incomingPackets.tryEmit(packet)
                }
            },
            onProfileUpdate = { profile ->
                val current = _knownNodes.value.toMutableMap()
                val existing = current[profile.id]
                current[profile.id] = if (existing != null) {
                    existing.copy(
                        name = if (profile.name != "Unknown User") profile.name else existing.name,
                        isOnline = profile.isOnline,
                        secondaryRouteAvailable = profile.secondaryRouteAvailable,
                        bestEndpoint = profile.bestEndpoint ?: existing.bestEndpoint,
                        reputation = profile.reputation,
                        isMaster = profile.isMaster
                    )
                } else profile
                _knownNodes.value = current
                gatewayManager?.onLocalNeighborUpdate(current.values.filter { !it.isProxied && it.isOnline })
            }
        )

        gatewayManager = GatewayManager(context, myNodeId, nickname, cloudTransport) { gatewayPacket ->
            engine?.sendPacket(gatewayPacket)
        }

        engine?.setStealth(isStealth)
        transport?.start(nickname, isStealth)
        gatewayManager?.start(isStealth)
    }

    private fun updatePresenceFromConnections(nodes: Map<String, String>) {
        val current = _knownNodes.value.toMutableMap()
        nodes.forEach { (endpointId, name) ->
            val nodeId = endpointId.split(":").lastOrNull() ?: endpointId
            if (nodeId != myNodeId) {
                val existing = current[nodeId] ?: UserProfile(id = nodeId, name = name)
                current[nodeId] = existing.copy(isOnline = true, bestEndpoint = endpointId, isProxied = false)
            }
        }
        _knownNodes.value = current
    }

    fun sendPacket(packet: Packet) {
        _totalPacketsSent.value++
        engine?.sendPacket(packet)
    }

    fun stop() {
        gatewayManager?.stop()
        transport?.stop()
        fileTransferManager?.stop()
        gatewayManager = null
        transport = null
        engine = null
        fileTransferManager = null
    }

    fun updateBattery(battery: Int) {
        engine?.updateMyBattery(battery)
    }

    fun getRoutingTable() = engine?.getRoutingTable()

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
