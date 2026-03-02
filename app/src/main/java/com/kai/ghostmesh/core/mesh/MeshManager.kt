package com.kai.ghostmesh.core.mesh

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.mesh.transports.GoogleNearbyTransport
import com.kai.ghostmesh.core.mesh.transports.BluetoothLegacyTransport
import com.kai.ghostmesh.core.mesh.transports.LanTransport
import com.kai.ghostmesh.core.mesh.transports.WifiDirectTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MeshManager(private val context: Context, private val myNodeId: String) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var engine: MeshEngine? = null
    private var transport: MultiTransportManager? = null
    private var gatewayManager: GatewayManager? = null

    val incomingPackets = MutableSharedFlow<Packet>(extraBufferCapacity = 100)
    val connectionUpdates = MutableStateFlow<List<UserProfile>>(emptyList())

    val totalPacketsSent = MutableStateFlow(0)
    val totalPacketsReceived = MutableStateFlow(0)

    fun startMesh(nickname: String, isStealth: Boolean = false) {
        if (engine != null) return

        val callback = object : MeshTransport.Callback {
            override fun onPacketReceived(endpointId: String, json: String) {
                totalPacketsReceived.update { it + 1 }
                engine?.processIncomingJson(endpointId, json)
            }

            override fun onConnectionChanged(nodes: Map<String, String>) {
                val profiles = nodes.map { (id, name) ->
                    UserProfile(id = id.split(":").last(), name = name, isOnline = true)
                }
                connectionUpdates.value = profiles
            }

            override fun onError(message: String) {}
        }

        transport = MultiTransportManager(callback)

        engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = nickname,
            onSendToNeighbors = { packet, except -> transport?.sendPacket(packet, except) },
            onHandlePacket = { scope.launch { incomingPackets.emit(it) } },
            onProfileUpdate = { id, name, status, battery, endpoint ->
                // Actual profile logic update would go here
            }
        )

        gatewayManager = GatewayManager(
            context = context,
            myNodeId = myNodeId,
            myNickname = nickname,
            onBroadcastGateway = { packet -> sendPacket(packet) }
        )

        if (isGooglePlayServicesAvailable(context)) {
            transport?.registerTransport(GoogleNearbyTransport(name = "Nearby", context = context, myNodeId = myNodeId, callback = callback))
        }
        transport?.registerTransport(BluetoothLegacyTransport(name = "Bluetooth", context = context, myNodeId = myNodeId, callback = callback))
        transport?.registerTransport(LanTransport(name = "LAN", context = context, myNodeId = myNodeId, callback = callback))
        transport?.registerTransport(WifiDirectTransport(name = "WiFiDirect", context = context, myNodeId = myNodeId, callback = callback))

        transport?.start(nickname, isStealth)
        gatewayManager?.start(isStealth)
    }

    fun sendPacket(packet: Packet) {
        totalPacketsSent.update { it + 1 }
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
