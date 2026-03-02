package com.kai.ghostmesh.core.mesh

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.core.mesh.transports.*
import com.kai.ghostmesh.core.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MeshManager(
    private val context: Context,
    private val myNodeId: String
) {

    private var transport: MultiTransportManager? = null
    private var engine: MeshEngine? = null
    private var gatewayManager: GatewayManager? = null

    private val _incomingPackets = MutableSharedFlow<Packet>()
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _connectionUpdates = MutableSharedFlow<Map<String, String>>()
    val connectionUpdates = _connectionUpdates.asSharedFlow()

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

            override fun onConnectionChanged(nodes: Map<String, String>) {
                _connectionUpdates.tryEmit(nodes)
            }

            override fun onError(message: String) {
            }
        }

        transport = MultiTransportManager(transportCallback)

        // Lazy loading of transports to save memory/battery
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

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = nickname,
            cacheSize = prefs.getInt("net_packet_cache", 2000),
            onSendToNeighbors = { packet, exceptId -> transport?.sendPacket(packet, exceptId) },
            onHandlePacket = {
                _totalPacketsReceived.update { it + 1 }
                _incomingPackets.tryEmit(it)
            },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        gatewayManager = GatewayManager(context, myNodeId, nickname, cloudTransport) { gatewayPacket ->
            engine?.sendPacket(gatewayPacket)
        }

        engine?.setStealth(isStealth)
        transport?.start(nickname, isStealth)
        gatewayManager?.start(isStealth)
    }

    fun sendPacket(packet: Packet) {
        _totalPacketsSent.update { it + 1 }
        engine?.sendPacket(packet)
    }

    fun stop() {
        gatewayManager?.stop()
        transport?.stop()
        gatewayManager = null
        transport = null
        engine = null
    }

    fun updateBattery(battery: Int) {
        engine?.updateMyBattery(battery)
        val intervalMs = if (battery > 50) 10000L else if (battery > 15) 30000L else 60000L
        transport?.setScanInterval(intervalMs)
    }

    fun getRoutingTable() = engine?.getRoutingTable()

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
