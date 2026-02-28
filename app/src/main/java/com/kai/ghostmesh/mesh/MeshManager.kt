package com.kai.ghostmesh.mesh

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.model.AppConfig
import com.kai.ghostmesh.model.Packet

class MeshManager(
    private val context: Context,
    private val myNodeId: String,
    private val myNickname: String,
    private val onPacketReceived: (Packet) -> Unit,
    private val onConnectionChanged: (Map<String, String>) -> Unit,
    private val onProfileUpdate: (String, String, String, Int, String?) -> Unit,
    private val onTransportError: (String) -> Unit
) {

    private val transport: MeshTransport
    private val engine: MeshEngine
    private var gatewayManager: GatewayManager? = null

    init {
        val prefs = context.getSharedPreferences(com.kai.ghostmesh.model.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val transportCallback = object : MeshTransport.Callback {
            override fun onPacketReceived(endpointId: String, json: String) {
                engine.processIncomingJson(endpointId, json)
            }

            override fun onConnectionChanged(nodes: Map<String, String>) {
                onConnectionChanged(nodes)
            }

            override fun onError(message: String) {
                onTransportError(message)
            }
        }

        val transports = mutableListOf<MeshTransport>()
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_NEARBY, true) && isGooglePlayServicesAvailable(context)) {
            transports.add(GoogleNearbyTransport(context, myNodeId, transportCallback))
        }
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_BLUETOOTH, true)) {
            transports.add(BluetoothLegacyTransport(context, myNodeId, transportCallback))
        }
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_LAN, true)) {
            transports.add(LanTransport(context, myNodeId, transportCallback))
        }
        if (prefs.getBoolean(AppConfig.KEY_ENABLE_WIFI_DIRECT, true)) {
            transports.add(WifiDirectTransport(context, myNodeId, transportCallback))
        }

        // Add Cloud Transport
        val cloudTransport = CloudTransport().apply { setNodeId(myNodeId) }
        transports.add(cloudTransport)

        transport = MultiTransportManager(transports, transportCallback)

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            cacheSize = prefs.getInt("net_packet_cache", 2000),
            onSendToNeighbors = { packet, exceptId -> transport.sendPacket(packet, exceptId) },
            onHandlePacket = { onPacketReceived(it) },
            onProfileUpdate = { id, name, status, battery, endpoint -> onProfileUpdate(id, name, status, battery, endpoint) }
        )

        gatewayManager = GatewayManager(context, myNodeId, myNickname) { gatewayPacket ->
            engine.sendPacket(gatewayPacket)
        }
    }

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        engine.setStealth(isStealth)
        transport.start(nickname, isStealth)
        gatewayManager?.start(isStealth)
    }

    fun sendPacket(packet: Packet) {
        engine.sendPacket(packet)
    }

    fun stop() {
        gatewayManager?.stop()
        transport.stop()
    }

    fun updateBattery(battery: Int) {
        engine.updateMyBattery(battery)
    }

    fun getRoutingTable() = engine.getRoutingTable()

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
