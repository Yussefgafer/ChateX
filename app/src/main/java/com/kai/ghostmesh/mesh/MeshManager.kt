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

    private val transport: MeshTransport
    private val engine: MeshEngine

    init {
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

        // Strategy: Prefer Google Nearby, Fallback to Legacy
        transport = if (isGooglePlayServicesAvailable(context)) {
            GoogleNearbyTransport(context, myNodeId, transportCallback)
        } else {
            BluetoothLegacyTransport(context, myNodeId, transportCallback)
        }

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, exceptId -> transport.sendPacket(packet, exceptId) },
            onHandlePacket = { onPacketReceived(it) },
            onProfileUpdate = { id, name, status -> onProfileUpdate(id, name, status) }
        )
    }

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        transport.start(nickname, isStealth)
    }

    fun sendPacket(packet: Packet) {
        transport.sendPacket(packet)
    }

    fun stop() {
        transport.stop()
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
