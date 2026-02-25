package com.kai.ghostmesh.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

class MeshManager(private val context: Context, private val onMessageReceived: (String, String) -> Unit) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val STRATEGY = Strategy.P2P_CLUSTER // Best for Mesh/Chat
    private val SERVICE_ID = "com.kai.ghostmesh.SERVICE_ID"
    
    private val connectedEndpoints = mutableSetOf<String>()

    fun startMesh(nickname: String) {
        startAdvertising(nickname)
        startDiscovery()
    }

    private fun startAdvertising(nickname: String) {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(nickname, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener { Log.d("Mesh", "Advertising started!") }
            .addOnFailureListener { e -> Log.e("Mesh", "Advertising failed", e) }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d("Mesh", "Discovery started!") }
            .addOnFailureListener { e -> Log.e("Mesh", "Discovery failed", e) }
    }

    fun sendMessage(content: String) {
        val payload = Payload.fromBytes(content.toByteArray(StandardCharsets.UTF_8))
        for (endpointId in connectedEndpoints) {
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Auto-accept connection for MVP
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            Log.d("Mesh", "Connection initiated with ${info.endpointName}")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                Log.d("Mesh", "Connected to $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            Log.d("Mesh", "Disconnected from $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("Mesh", "Endpoint found: ${info.endpointName}, requesting connection...")
            connectionsClient.requestConnection("GhostUser", endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                val message = String(it, StandardCharsets.UTF_8)
                onMessageReceived(endpointId, message)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
