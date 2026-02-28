package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Test

class PathfindingTest {

    private val gson = Gson()

    @Test
    fun `test optimal path selection based on cumulative cost`() {
        val sentPackets = mutableListOf<Pair<Packet, String?>>()

        val engine = MeshEngine(
            myNodeId = "NODE_B",
            myNickname = "RelayB",
            onSendToNeighbors = { packet, endpoint -> sentPackets.add(packet to endpoint) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        // Mock 1: Route to NODE_E through NODE_C (Total Path Cost: 2)
        val packetFromC = Packet(
            senderId = "NODE_C",
            senderName = "C",
            receiverId = "ALL",
            type = PacketType.BATTERY_HEARTBEAT,
            payload = "",
            pathCost = 1f, // E -> C is 1
            senderBattery = 100
        )
        engine.processIncomingJson("LAN:ENDPOINT_C", gson.toJson(packetFromC))

        // Mock 2: Route to NODE_E through NODE_D (Total Path Cost: 20)
        val packetFromD = Packet(
            senderId = "NODE_D",
            senderName = "D",
            receiverId = "ALL",
            type = PacketType.BATTERY_HEARTBEAT,
            payload = "",
            pathCost = 10f, // E -> D is 10
            senderBattery = 100
        )
        engine.processIncomingJson("Bluetooth:ENDPOINT_D", gson.toJson(packetFromD))

        // Act: Send a packet to NODE_E
        val packetForE = Packet(
            senderId = "NODE_A",
            senderName = "A",
            receiverId = "NODE_C", // In a real scenario, A would send to B first
            type = PacketType.CHAT,
            payload = "Secret for E"
        )
        // Simulate receiving from A
        engine.processIncomingJson("LAN:ENDPOINT_A", gson.toJson(packetForE.copy(receiverId = "NODE_B")))

        // Wait... actually the routing table stores the *sender's* path.
        // To route TO E, B needs to have seen a packet FROM E.

        val packetFromEViaC = Packet(senderId = "NODE_E", senderName = "E", type = PacketType.BATTERY_HEARTBEAT, payload = "", pathCost = 1f, senderBattery = 100)
        engine.processIncomingJson("LAN:ENDPOINT_C", gson.toJson(packetFromEViaC))

        val packetFromEViaD = Packet(senderId = "NODE_E", senderName = "E", type = PacketType.BATTERY_HEARTBEAT, payload = "", pathCost = 10f, senderBattery = 100)
        engine.processIncomingJson("Bluetooth:ENDPOINT_D", gson.toJson(packetFromEViaD))

        // Now send to E
        sentPackets.clear()
        val realPacketForE = Packet(senderId = "NODE_B", senderName = "B", receiverId = "NODE_E", type = PacketType.CHAT, payload = "Hi E")
        engine.sendPacket(realPacketForE)

        // Assert: B should pick the path through C (LAN)
        assertEquals("Should route through C due to lower cost", "LAN:ENDPOINT_C", sentPackets[0].second)
    }

    @Test
    fun `test battery-aware routing avoidance`() {
        val sentPackets = mutableListOf<Pair<Packet, String?>>()
        val engine = MeshEngine(
            myNodeId = "NODE_B",
            myNickname = "RelayB",
            onSendToNeighbors = { packet, endpoint -> sentPackets.add(packet to endpoint) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        // Path 1: Through C (LAN, but C has 10% battery)
        val packetFromC = Packet(senderId = "NODE_C", senderName = "C", type = PacketType.BATTERY_HEARTBEAT, payload = "", pathCost = 0f, senderBattery = 10)
        engine.processIncomingJson("LAN:ENDPOINT_C", gson.toJson(packetFromC))

        // Path 2: Through D (WiFiDirect, 100% battery)
        val packetFromD = Packet(senderId = "NODE_D", senderName = "D", type = PacketType.BATTERY_HEARTBEAT, payload = "", pathCost = 0f, senderBattery = 100)
        engine.processIncomingJson("WiFiDirect:ENDPOINT_D", gson.toJson(packetFromD))

        // Send to C
        sentPackets.clear()
        engine.sendPacket(Packet(senderId = "NODE_B", senderName = "B", receiverId = "NODE_C", type = PacketType.CHAT, payload = "Low Bat?"))

        // Cost LAN (1) * Penalty (5) = 5
        // Cost WiFiDirect (2) = 2
        // If we were routing *to* C, we use the direct link.
        // Let's route to E where C and D are potential relays.

        val packetFromEViaC = Packet(senderId = "NODE_E", senderName = "E", type = PacketType.BATTERY_HEARTBEAT, payload = "", pathCost = 0f, senderBattery = 10)
        engine.processIncomingJson("LAN:ENDPOINT_C", gson.toJson(packetFromEViaC)) // Cost 1*5 = 5

        val packetFromEViaD = Packet(senderId = "NODE_E", senderName = "E", type = PacketType.BATTERY_HEARTBEAT, payload = "", pathCost = 0f, senderBattery = 100)
        engine.processIncomingJson("WiFiDirect:ENDPOINT_D", gson.toJson(packetFromEViaD)) // Cost 2

        sentPackets.clear()
        engine.sendPacket(Packet(senderId = "NODE_B", senderName = "B", receiverId = "NODE_E", type = PacketType.CHAT, payload = "Hi E"))

        assertEquals("Should avoid low-battery node C even if on LAN", "WiFiDirect:ENDPOINT_D", sentPackets[0].second)
    }
}
