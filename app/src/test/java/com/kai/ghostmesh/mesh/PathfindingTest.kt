package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PathfindingTest {

    private lateinit var engine: MeshEngine
    private val neighborPackets = mutableListOf<Pair<Packet, String?>>()

    @Before
    fun setup() {
        neighborPackets.clear()
        engine = MeshEngine(
            myNodeId = "me",
            myNickname = "Me",
            onSendToNeighbors = { packet, exceptId -> neighborPackets.add(packet to exceptId) },
            onHandlePacket = {},
            onProfileUpdate = { _, _, _, _, _ -> }
        )
    }

    @Test
    fun routingTableUpdatesWithNewRoutes() {
        val packet = Packet(senderId = "nodeA", senderName = "A", type = PacketType.CHAT, payload = "Test", hopCount = 2)
        val json = com.google.gson.Gson().toJson(packet)

        engine.processIncomingJson("neighborB", json)

        val table = engine.getRoutingTable()
        assertTrue(table.containsKey("nodeA"))
        assertEquals("neighborB", table["nodeA"]?.nextHopEndpointId)
    }
}
