package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
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
            onProfileUpdate = { _, _, _, _, _ -> }
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
        engine.processIncomingJson("LAN:ENDPOINT_A", jsonFromA)

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
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        val packet = Packet(senderId = "A", senderName = "A", type = PacketType.CHAT, payload = "Loop")
        val json = gson.toJson(packet)

        // Act: Receive same packet twice
        engine.processIncomingJson("LAN:E1", json)
        engine.processIncomingJson("LAN:E1", json)

        // Assert: Should only relay once
        assertEquals("Should only relay once even if received twice", 1, relayedPackets.size)
    }

    @Test
    fun `test spectral routing cost calculation`() {
        val relayedPackets = mutableListOf<Packet>()
        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, _ -> relayedPackets.add(packet) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        // A sends to B via LAN (Low cost)
        val packetLAN = Packet(senderId = "A", senderName = "A", type = PacketType.BATTERY_HEARTBEAT, payload = "", senderBattery = 100)
        engine.processIncomingJson("LAN:E1", gson.toJson(packetLAN))

        val routeLAN = engine.getRoutingTable()["A"]
        assertEquals(1f, routeLAN?.cost ?: 0f, 0.1f)

        // A sends to B via Bluetooth (High cost)
        // Note: Deduplication would stop this if ID is same, so use new ID or different node
        val packetBT = Packet(senderId = "C", senderName = "C", type = PacketType.BATTERY_HEARTBEAT, payload = "", senderBattery = 100)
        engine.processIncomingJson("Bluetooth:E2", gson.toJson(packetBT))

        val routeBT = engine.getRoutingTable()["C"]
        assertEquals(10f, routeBT?.cost ?: 0f, 0.1f)
    }
}
