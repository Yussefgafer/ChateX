package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType

/**
 * 🚀 The "Brain" of the Mesh Network.
 * Pure logic, no Android dependencies, 100% testable.
 */
class MeshEngine(
    private val myNodeId: String,
    private val myNickname: String,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String) -> Unit // id, name, status
) {
    private val processedPacketIds = mutableSetOf<String>()
    private val gson = Gson()

    fun processIncomingJson(fromEndpointId: String, json: String) {
        val packet = try {
            gson.fromJson(json, Packet::class.java)
        } catch (e: Exception) {
            return 
        } ?: return

        // 1. Deduplication: Don't process twice
        if (processedPacketIds.contains(packet.id)) return
        processedPacketIds.add(packet.id)

        // 2. Logic: Is it for me?
        val isForMe = packet.receiverId == myNodeId || packet.receiverId == "ALL"
        if (isForMe) {
            when (packet.type) {
                PacketType.PROFILE_SYNC -> {
                    val parts = packet.payload.split("|")
                    onProfileUpdate(packet.senderId, parts.getOrNull(0) ?: "Unknown", parts.getOrNull(1) ?: "")
                }
                else -> onHandlePacket(packet)
            }
        }

        // 3. Relay Logic: Do I need to pass it on?
        val shouldRelay = packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)
        if (shouldRelay) {
            val relayPacket = packet.copy(hopCount = packet.hopCount - 1)
            onSendToNeighbors(relayPacket, fromEndpointId)
        }
    }

    fun createChatPacket(receiverId: String, content: String, isEncrypted: Boolean): Packet {
        return Packet(
            senderId = myNodeId,
            senderName = myNickname,
            receiverId = receiverId,
            type = PacketType.CHAT,
            payload = content
        ).also { processedPacketIds.add(it.id) }
    }
}
