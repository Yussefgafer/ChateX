package com.kai.ghostmesh.mesh

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.model.Packet

class MeshManager(
    private val context: Context,
    private val myNodeId: String,
    private val myNickname: String,
    private val onPacketReceived: (Packet) -> Unit,
    private val onConnectionChanged: (Map<String, String>) -> Unit,
    private val onProfileUpdate: (String, String, String) -> Unit,
    private val onTransportError: (String) -> Unit
) {

    private val transports = mutableListOf<MeshTransport>()
    private val engine: MeshEngine
    private val allConnections = java.util.concurrent.ConcurrentHashMap<String, String>()

    init {
        val transportCallback = object : MeshTransport.Callback {
            override fun onPacketReceived(endpointId: String, json: String) {
                engine.processIncomingJson(endpointId, json)
            }

            override fun onConnectionChanged(nodes: Map<String, String>) {
                allConnections.putAll(nodes)
                onConnectionChanged(allConnections.toMap())
            }

            override fun onError(message: String) {
                onTransportError(message)
            }
        }

        // Initialize Hybrid Transports
        if (isGooglePlayServicesAvailable(context)) {
            transports.add(GoogleNearbyTransport(context, myNodeId, transportCallback))
        }

        transports.add(BluetoothLegacyTransport(context, myNodeId, transportCallback))
        transports.add(LanTransport(context, myNodeId, transportCallback))

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, exceptId ->
                transports.forEach { it.sendPacket(packet, exceptId) }
            },
            onHandlePacket = { onPacketReceived(it) },
            onProfileUpdate = { id, name, status -> onProfileUpdate(id, name, status) }
        )
    }

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        transports.forEach { it.start(nickname, isStealth) }
    }

    fun sendPacket(packet: Packet) {
        transports.forEach { it.sendPacket(packet) }
    }

    fun stop() {
        transports.forEach { it.stop() }
        allConnections.clear()
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
