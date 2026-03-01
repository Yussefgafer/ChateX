package com.kai.ghostmesh.core.mesh

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
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

    private fun checkInternet() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                         capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

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

    fun isGateway(): Boolean = isInternetAvailable && !isStealth
}
