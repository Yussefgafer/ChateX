package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Constants
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType

class MeshEngine(
    private val myNodeId: String,
    private val myNickname: String,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String) -> Unit
) {
    private val processedPacketIds = object : LinkedHashSet<String>(Constants.MAX_PROCESSED_PACKETS) {
        override fun add(element: String): Boolean {
            if (size >= Constants.MAX_PROCESSED_PACKETS) {
                val first = iterator().next()
                remove(first)
            }
            return super.add(element)
        }
    }
    private val gson = Gson()
    fun processIncomingJson(fromEndpointId: String, json: String) {
        val packet = try {
            gson.fromJson(json, Packet::class.java)
        } catch (e: Exception) { return } ?: return

        if (processedPacketIds.contains(packet.id) && packet.type != PacketType.ACK) return
        processedPacketIds.add(packet.id)

        val isForMe = packet.receiverId == myNodeId || packet.receiverId == "ALL"
        
        if (isForMe) {
            when (packet.type) {
                PacketType.PROFILE_SYNC -> {
                    val parts = packet.payload.split("|")
                    onProfileUpdate(packet.senderId, parts.getOrNull(0) ?: "Unknown", parts.getOrNull(1) ?: "")
                }
                PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE, PacketType.ACK, PacketType.TYPING_START, PacketType.TYPING_STOP, PacketType.REACTION -> {
                    onHandlePacket(packet)
                    
                    if ((packet.type == PacketType.CHAT || packet.type == PacketType.IMAGE || packet.type == PacketType.VOICE || packet.type == PacketType.FILE) && packet.receiverId != "ALL") {
                        val ack = Packet(
                            senderId = myNodeId, senderName = myNickname, receiverId = packet.senderId,
                            type = PacketType.ACK, payload = packet.id
                        )
                        onSendToNeighbors(ack, null)
                    }
                }
                PacketType.LAST_SEEN -> {
                    onHandlePacket(packet)
                }
                PacketType.PROFILE_IMAGE -> {
                    onHandlePacket(packet)
                }
            }
        }

        val shouldRelay = packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)
        if (shouldRelay) {
            onSendToNeighbors(packet.copy(hopCount = packet.hopCount - 1), fromEndpointId)
        }
    }
}
