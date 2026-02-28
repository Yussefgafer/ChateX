package com.kai.ghostmesh.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import java.nio.charset.StandardCharsets

class GoogleNearbyTransport(
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val SERVICE_ID = "com.kai.chatex.SERVICE_ID"

    private val connectedEndpoints = mutableSetOf<String>()
    private val nodeIdToName = mutableMapOf<String, String>()

    override fun start(nickname: String, isStealth: Boolean) {
        if (!isStealth) {
            val optionsAdv = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            connectionsClient.startAdvertising(nickname, SERVICE_ID, connectionLifecycleCallback, optionsAdv)
                .addOnFailureListener { callback.onError("Advertising failed: ${it.message}") }
        }
        
        val optionsDisc = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, optionsDisc)
            .addOnFailureListener { callback.onError("Discovery failed: ${it.message}") }
    }

    override fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        nodeIdToName.clear()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
        
        if (endpointId != null) {
            connectionsClient.sendPayload(endpointId, payload)
        } else {
            for (id in connectedEndpoints) {
                connectionsClient.sendPayload(id, payload)
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            nodeIdToName[endpointId] = info.endpointName
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                callback.onConnectionChanged(nodeIdToName.toMap())
            } else {
                nodeIdToName.remove(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            nodeIdToName.remove(endpointId)
            callback.onConnectionChanged(nodeIdToName.toMap())
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
                callback.onPacketReceived(endpointId, String(it, StandardCharsets.UTF_8))
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
