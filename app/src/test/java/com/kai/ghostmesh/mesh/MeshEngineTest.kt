package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshEngineTest {

    private val gson = Gson()
    private val myNodeId = "NODE_B"
    private val myNickname = "RelayNode"

    @Test
    fun `test packet relay from A to C through B`() {
        // Arrange
        val relayedPackets = mutableListOf<Packet>()
        val handledPackets = mutableListOf<Packet>()

        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, _ -> relayedPackets.add(packet) },
            onHandlePacket = { packet -> handledPackets.add(packet) },
            onProfileUpdate = { _, _, _ -> }
        )

        // Packet from A meant for C
        val packetFromA = Packet(
            id = "MSG_1",
            senderId = "NODE_A",
            senderName = "UserA",
            receiverId = "NODE_C",
            type = PacketType.CHAT,
            payload = "Hello C!",
            hopCount = 3
        )
        val jsonFromA = gson.toJson(packetFromA)

        // Act: B receives the packet from A
        engine.processIncomingJson("ENDPOINT_A", jsonFromA)

        // Assert
        // 1. B shouldn't "handle" the packet (because it's for C)
        assertTrue("B should not handle packet meant for C", handledPackets.isEmpty())

        // 2. B should relay the packet to neighbors
        assertEquals("B should relay 1 packet", 1, relayedPackets.size)
        assertEquals("Relayed packet should have decreased hopCount", 2, relayedPackets[0].hopCount)
        assertEquals("Relayed packet should maintain original content", "Hello C!", relayedPackets[0].payload)
    }

    @Test
    fun `test deduplication prevents infinite loops`() {
        val relayedPackets = mutableListOf<Packet>()
        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, _ -> relayedPackets.add(packet) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _ -> }
        )

        val packet = Packet(senderId = "A", senderName = "A", type = PacketType.CHAT, payload = "Loop")
        val json = gson.toJson(packet)

        // Act: Receive same packet twice
        engine.processIncomingJson("E1", json)
        engine.processIncomingJson("E1", json)

        // Assert: Should only relay once
        assertEquals("Should only relay once even if received twice", 1, relayedPackets.size)
    }
}
