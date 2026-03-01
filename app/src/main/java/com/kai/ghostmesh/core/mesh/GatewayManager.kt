package com.kai.ghostmesh.core.mesh

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.mesh.transports.CloudTransport
import kotlinx.coroutines.*

class GatewayManager(
    private val context: Context,
    private val myNodeId: String,
    private val myNickname: String,
    private val cloudTransport: CloudTransport? = null,
    private val onBroadcastGateway: (Packet) -> Unit
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInternetAvailable = false
    private var isStealth = false
    private val gson = Gson()

    // Mission: The Mailbox Protocol - Mailbox Limit
    private val MAX_PROXIED_NEIGHBORS = 10
    private var localNeighbors = emptyList<UserProfile>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            checkInternet()
        }

        override fun onLost(network: Network) {
            checkInternet()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            checkInternet()
        }
    }

    fun start(isStealth: Boolean) {
        this.isStealth = isStealth
        if (isStealth) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        checkInternet()
    }

    fun stop() {
        if (!isStealth) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {}
        }
        cloudTransport?.stop()
        scope.cancel()
    }

    fun onLocalNeighborUpdate(neighbors: List<UserProfile>) {
        if (!isGateway()) return

        // Prioritize by most recently active (already sorted by MeshManager flow ideally, but we ensure limit)
        val selected = neighbors.take(MAX_PROXIED_NEIGHBORS)

        if (selected.map { it.id }.toSet() != localNeighbors.map { it.id }.toSet()) {
            localNeighbors = selected
            Log.d("GatewayManager", "Updating Mailbox for ${selected.size} neighbors")

            // 1. Presence Proxying: Send NEIGHBOR_LIST to Cloud
            broadcastNeighborList(selected)

            // 2. Proxy Subscription: Update CloudTransport REQ
            cloudTransport?.updateProxiedNodes(selected.map { it.id })
        }
    }

    private fun checkInternet() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        if (hasInternet != isInternetAvailable) {
            isInternetAvailable = hasInternet
            if (isInternetAvailable) {
                cloudTransport?.start(myNickname, isStealth)
                broadcastGatewayPresence()
            } else {
                cloudTransport?.stop()
            }
        }
    }

    private fun broadcastGatewayPresence() {
        scope.launch {
            val packet = Packet(
                senderId = myNodeId,
                senderName = myNickname,
                receiverId = "ALL",
                type = PacketType.GATEWAY_AVAILABLE,
                payload = "ACTIVE",
                hopCount = 1
            )
            onBroadcastGateway(packet)
        }
    }

    private fun broadcastNeighborList(neighbors: List<UserProfile>) {
        if (!isGateway()) return
        scope.launch {
            val neighborInfos = neighbors.map {
                NeighborInfo(it.id, it.name, it.batteryLevel, it.color)
            }
            val packet = Packet(
                senderId = myNodeId,
                senderName = myNickname,
                receiverId = "ALL",
                type = PacketType.NEIGHBOR_LIST,
                payload = gson.toJson(neighborInfos),
                hopCount = 1 // Broadcast presence to cloud primarily
            )
            onBroadcastGateway(packet)
        }
    }

    fun isGateway(): Boolean = isInternetAvailable && !isStealth
}
