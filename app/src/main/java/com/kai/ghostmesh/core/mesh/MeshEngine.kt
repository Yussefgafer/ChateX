package com.kai.ghostmesh.core.mesh

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kai.ghostmesh.*
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.security.SecureRandom

data class Route(
    val destinationId: String,
    val nextHopEndpointId: String,
    val cost: Float,
    val battery: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class MeshEngine(
    private val myNodeId: String,
    private val myNickname: String,
    private val cacheSize: Int = 1000,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (UserProfile) -> Unit
) {
    private val PROTOCOL_VERSION = 2
    private val FIXED_PACKET_SIZE = 1024

    private val packetCache = Array<String?>(cacheSize) { null }
    private var cacheIndex = 0

    private val multiPathRoutingTable = ConcurrentHashMap<String, MutableList<Route>>()
    private val gatewayNodes = ConcurrentHashMap<String, Long>()
    private val gson = Gson()
    private var myBattery: Int = 100
    private var isStealth: Boolean = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lamportClock = AtomicLong(0)
    private var myReputation = 0
    private val clusterNeighbors = ConcurrentHashMap<String, Int>()
    private var masterNodeId: String? = null

    // Mission: Master Authority - Torrent Tracking
    private val globalBitfields = ConcurrentHashMap<String, MutableSet<String>>() // InfoHash to NodeIds

    init {
        startGossipTimer()
        startHeartbeatTimer()
    }

    private fun isPacketSeen(id: String): Boolean {
        synchronized(packetCache) {
            for (cachedId in packetCache) { if (cachedId == id) return true }
            packetCache[cacheIndex] = id
            cacheIndex = (cacheIndex + 1) % cacheSize
            return false
        }
    }

    fun tickClock(): Long = lamportClock.incrementAndGet()
    private fun syncClock(remoteTime: Long) {
        var current: Long; var next: Long
        do { current = lamportClock.get(); next = Math.max(current, remoteTime) + 1 } while (!lamportClock.compareAndSet(current, next))
    }

    private fun startGossipTimer() { scope.launch { while (isActive) { delay(30000); broadcastTopologyUpdate() } } }
    private fun startHeartbeatTimer() { scope.launch { while (isActive) { delay(5000); sendKeepAlive(); validateRoutes() } } }

    private fun sendKeepAlive() {
        val packet = Packet(senderId = myNodeId, senderName = myNickname, receiverId = "ALL", type = PacketType.KEEP_ALIVE, payload = "PULSE", hopCount = 1, lamportTime = tickClock(), reputation = myReputation)
        sendPacketInternal(packet)
    }

    private fun validateRoutes() {
        val now = System.currentTimeMillis()
        multiPathRoutingTable.forEach { (destId, routes) ->
            val initialCount = routes.size
            routes.removeIf { now - it.timestamp > 12000 }
            if (routes.size < initialCount) updateProfilePresence(destId)
        }
        runElection()
    }

    private fun runElection() {
        val candidates = clusterNeighbors.toMutableMap(); candidates[myNodeId] = myReputation
        val newMaster = candidates.entries.maxByOrNull { it.value }?.key
        if (newMaster != masterNodeId) {
            masterNodeId = newMaster
            Log.d("MeshEngine", "Cluster Master: $masterNodeId")
        }
    }

    private fun broadcastTopologyUpdate() {
        val routeSummary = multiPathRoutingTable.mapValues { it.value.firstOrNull()?.cost ?: 1000f }.toList().sortedBy { it.second }.take(10).toMap()
        val packet = Packet(senderId = myNodeId, senderName = myNickname, receiverId = "ALL", type = PacketType.TOPOLOGY_UPDATE, payload = gson.toJson(routeSummary), hopCount = 1, lamportTime = tickClock())
        sendPacketInternal(packet)
    }

    fun updateMyBattery(battery: Int) { myBattery = battery }
    fun setStealth(stealth: Boolean) { this.isStealth = stealth }
    fun getRoutingTable(): Map<String, Route> = multiPathRoutingTable.mapValues { it.value.first() }

    fun processIncomingJson(fromEndpointId: String, json: String) {
        try {
            val packet = gson.fromJson(json, Packet::class.java)
            if (packet == null || !packet.isValid()) return
            if (packet.protocolVersion > PROTOCOL_VERSION) return
            if (isPacketSeen(packet.id)) {
                if (packet.hopCount >= 2) updateRouteHeartbeat(packet.senderId, fromEndpointId, packet.reputation)
                return
            }
            syncClock(packet.lamportTime)
            processIncomingPacket(fromEndpointId, packet)
        } catch (e: Exception) { }
    }

    fun processIncomingBinary(fromEndpointId: String, data: ByteArray) {
        processIncomingJson(fromEndpointId, String(data))
    }

    private fun updateRouteHeartbeat(senderId: String, endpointId: String, reputation: Int) {
        clusterNeighbors[senderId] = reputation
        multiPathRoutingTable[senderId]?.find { it.nextHopEndpointId == endpointId }?.let { multiPathRoutingTable[senderId]?.remove(it); multiPathRoutingTable[senderId]?.add(0, it.copy(timestamp = System.currentTimeMillis())) }
    }

    private fun processIncomingPacket(fromEndpointId: String, packet: Packet) {
        val transport = fromEndpointId.split(":").firstOrNull() ?: "Unknown"
        val linkCost = calculateLinkCost(transport, packet.senderBattery); val totalPathCost = packet.pathCost + linkCost
        clusterNeighbors[packet.senderId] = packet.reputation
        val routes = multiPathRoutingTable.getOrPut(packet.senderId) { mutableListOf() }
        val existingRoute = routes.find { it.nextHopEndpointId == fromEndpointId }
        if (existingRoute == null) routes.add(Route(packet.senderId, fromEndpointId, totalPathCost, packet.senderBattery))
        else { routes.remove(existingRoute); routes.add(Route(packet.senderId, fromEndpointId, totalPathCost, packet.senderBattery)) }
        routes.sortBy { it.cost }; if (routes.size > 3) routes.removeAt(routes.size - 1)
        val isForMe = packet.receiverId == myNodeId || packet.receiverId == "ALL"
        if (isForMe) {
            when (packet.type) {
                PacketType.TOPOLOGY_UPDATE -> handleTopologyUpdate(packet, totalPathCost, fromEndpointId)
                PacketType.GATEWAY_AVAILABLE -> { gatewayNodes[packet.senderId] = System.currentTimeMillis(); myReputation += 10 }
                PacketType.BITFIELD -> handleBitfieldTracking(packet)
                PacketType.PROFILE_SYNC -> { val parts = packet.payload.split("|"); onProfileUpdate(UserProfile(id = packet.senderId, name = parts.getOrNull(0) ?: "Unknown", status = parts.getOrNull(1) ?: "", batteryLevel = packet.senderBattery, bestEndpoint = fromEndpointId, isOnline = true, secondaryRouteAvailable = routes.size > 1, reputation = packet.reputation, isMaster = packet.senderId == masterNodeId)) }
                PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE -> { onHandlePacket(packet); if (packet.receiverId != "ALL" && shouldAck(packet.type)) { sendPacket(Packet(senderId = myNodeId, senderName = myNickname, receiverId = packet.senderId, type = PacketType.ACK, payload = packet.id, lamportTime = tickClock())) } }
                else -> onHandlePacket(packet)
            }
        }
        if (packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)) { myReputation += 1; val relayedPacket = packet.copy(hopCount = packet.hopCount - 1, pathCost = totalPathCost, senderBattery = myBattery, lamportTime = tickClock()); relayPacket(relayedPacket, fromEndpointId) }
    }

    private fun handleBitfieldTracking(packet: Packet) {
        if (masterNodeId == myNodeId) {
            val parts = packet.payload.split("|")
            if (parts[0] == "SEED") {
                val infoHash = parts[1]
                globalBitfields.getOrPut(infoHash) { mutableSetOf() }.add(packet.senderId)
                Log.d("MeshEngine", "Master Tracker: Ghost ${packet.senderId} is seeding $infoHash")
            }
        }
        onHandlePacket(packet)
    }

    private fun handleTopologyUpdate(packet: Packet, pathCostToGossipSource: Float, fromEndpointId: String) {
        try {
            val type = object : TypeToken<Map<String, Float>>() {}.type; val remoteRoutes: Map<String, Float> = gson.fromJson(packet.payload, type)
            remoteRoutes.forEach { (destId, remoteCost) -> if (destId == myNodeId) return@forEach; val totalCost = pathCostToGossipSource + remoteCost + 2f; val routes = multiPathRoutingTable.getOrPut(destId) { mutableListOf() }; if (routes.none { it.nextHopEndpointId == fromEndpointId }) { routes.add(Route(destId, fromEndpointId, totalCost, 100)); routes.sortBy { it.cost }; updateProfilePresence(destId) } }
        } catch (e: Exception) {}
    }

    private fun updateProfilePresence(destId: String) {
        val routes = multiPathRoutingTable[destId] ?: return; if (routes.isEmpty()) return
        onProfileUpdate(UserProfile(id = destId, isOnline = true, secondaryRouteAvailable = routes.size > 1, bestEndpoint = routes.first().nextHopEndpointId, isMaster = destId == masterNodeId, reputation = clusterNeighbors[destId] ?: 0))
    }

    private fun relayPacket(packet: Packet, fromEndpointId: String?) {
        if (packet.receiverId == "ALL") onSendToNeighbors(packet, fromEndpointId)
        else { val routes = multiPathRoutingTable[packet.receiverId]; val bestRoute = routes?.firstOrNull { it.nextHopEndpointId != fromEndpointId }; if (bestRoute != null) onSendToNeighbors(packet, bestRoute.nextHopEndpointId); else if (gatewayNodes.isNotEmpty()) tunnelToGateway(packet); else onSendToNeighbors(packet, fromEndpointId) }
    }

    fun sendPacket(packet: Packet) {
        val packetWithMeta = packet.copy(senderBattery = myBattery, pathCost = 0f, lamportTime = tickClock(), reputation = myReputation, protocolVersion = PROTOCOL_VERSION); sendPacketInternal(packetWithMeta)
    }

    private fun sendPacketInternal(packet: Packet) {
        if (packet.receiverId == "ALL") onSendToNeighbors(packet, null)
        else { val routes = multiPathRoutingTable[packet.receiverId]; val bestRoute = routes?.firstOrNull(); if (bestRoute != null) onSendToNeighbors(packet, bestRoute.nextHopEndpointId); else if (gatewayNodes.isNotEmpty()) tunnelToGateway(packet); else onSendToNeighbors(packet, null) }
    }

    private fun tunnelToGateway(packet: Packet) {
        val bestGatewayId = gatewayNodes.keys().toList().firstOrNull() ?: return; val routes = multiPathRoutingTable[bestGatewayId]; val gatewayRoute = routes?.firstOrNull() ?: return; val tunnelPacket = Packet(senderId = myNodeId, senderName = myNickname, receiverId = bestGatewayId, type = PacketType.TUNNEL, payload = gson.toJson(packet), hopCount = 3, lamportTime = tickClock())
        onSendToNeighbors(tunnelPacket, gatewayRoute.nextHopEndpointId)
    }

    private fun calculateLinkCost(transport: String, battery: Int): Float { var cost = when (transport) { "LAN" -> 1f; "WiFiDirect" -> 2f; "Nearby" -> 5f; "Bluetooth" -> 10f; "Cloud" -> 50f; else -> 15f }; if (battery < 15) cost *= 5f else if (battery < 30) cost *= 2f; return cost }
    private fun shouldAck(type: PacketType): Boolean = when(type) { PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE -> true; else -> false }
}
