package com.kai.ghostmesh.core.mesh.transports

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.util.GhostLog as Log

class GoogleNearbyTransport(
    override val name: String = "Nearby",
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    private val STRATEGY = Strategy.P2P_CLUSTER
    private val SERVICE_ID = "com.kai.ghostmesh.SERVICE_ID"
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val discoveredEndpoints = mutableMapOf<String, String>()

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun start(nickname: String, isStealth: Boolean) {
        startAdvertising(nickname)
        startDiscovery()
    }

    private fun startAdvertising(nickname: String) {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(nickname, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnFailureListener { Log.e(name, "Advertising failed", it) }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnFailureListener { Log.e(name, "Discovery failed", it) }
    }

    override fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        discoveredEndpoints.clear()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val data = com.google.gson.Gson().toJson(packet).toByteArray(Charsets.UTF_8)
        val payload = Payload.fromBytes(data)
        if (endpointId != null) {
            connectionsClient.sendPayload(endpointId, payload)
        } else {
            discoveredEndpoints.keys.forEach { connectionsClient.sendPayload(it, payload) }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                discoveredEndpoints[endpointId] = endpointId
                callback.onConnectionChanged(discoveredEndpoints.toMap())
            }
        }
        override fun onDisconnected(endpointId: String) {
            discoveredEndpoints.remove(endpointId)
            callback.onConnectionChanged(discoveredEndpoints.toMap())
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
            payload.asBytes()?.let { callback.onPacketReceived(endpointId, String(it, Charsets.UTF_8)) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
