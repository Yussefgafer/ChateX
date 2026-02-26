package com.kai.ghostmesh.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import java.nio.charset.StandardCharsets

class MeshManager(
    private val context: Context,
    private val myNodeId: String,
    private val myNickname: String,
    private val onPacketReceived: (Packet) -> Unit,
    private val onConnectionChanged: (Map<String, String>) -> Unit,
    private val onProfileUpdate: (String, String, String) -> Unit
) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val SERVICE_ID = "com.kai.chatex.SERVICE_ID"

    private val connectedEndpoints = mutableSetOf<String>()
    private val endpointIdToNodeId = mutableMapOf<String, String>()
    private val nodeIdToName = mutableMapOf<String, String>()

    // 🚀 The Tested Brain
    private val engine = MeshEngine(
        myNodeId = myNodeId,
        myNickname = myNickname,
        onSendToNeighbors = { packet, exceptId -> relayPacket(packet, exceptId) },
        onHandlePacket = { onPacketReceived(it) },
        onProfileUpdate = { id, name, status -> onProfileUpdate(id, name, status) }
    )

    fun startMesh(nickname: String) {
        connectionsClient.startAdvertising(nickname, SERVICE_ID, connectionLifecycleCallback, AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
    }

    fun sendPacket(packet: Packet) {
        val json = gson.toJson(packet)
        val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
        for (endpointId in connectedEndpoints) {
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private fun relayPacket(packet: Packet, exceptEndpointId: String?) {
        val json = gson.toJson(packet)
        val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
        for (endpointId in connectedEndpoints) {
            if (endpointId != exceptEndpointId) {
                connectionsClient.sendPayload(endpointId, payload)
            }
        }
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        onConnectionChanged(emptyMap())
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            val nodeId = endpointIdToNodeId.remove(endpointId)
            if (nodeId != null) {
                nodeIdToName.remove(nodeId)
                onConnectionChanged(nodeIdToName.toMap())
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(myNodeId, endpointId, connectionLifecycleCallback)
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                val json = String(it, StandardCharsets.UTF_8)
                engine.processIncomingJson(endpointId, json)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
