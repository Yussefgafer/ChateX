package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class PathfindingTest {

    private lateinit var engine: MeshEngine
    private val neighborPackets = mutableListOf<Pair<Packet, String?>>()

    @Before
    fun setup() {
        mockkObject(SecurityManager)
        every { SecurityManager.verifyPacket(any(), any(), any(), any()) } returns true
    }

    @Test
    fun routingTableUpdatesWithNewRoutes() = runTest {
        neighborPackets.clear()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        engine = MeshEngine(
            myNodeId = "me",
            myNickname = "Me",
            onSendToNeighbors = { packet, exceptId -> neighborPackets.add(packet to exceptId) },
            onHandlePacket = {},
            onProfileUpdate = { _, _, _, _, _ -> },
            dispatcher = testDispatcher
        )

        val packet = Packet(
            senderId = "nodeA",
            senderName = "A",
            type = PacketType.CHAT,
            payload = "Test",
            hopCount = 2,
            signature = "valid"
        )
        val json = com.google.gson.Gson().toJson(packet)

        engine.processIncomingJson("neighborB", json)

        val table = engine.getRoutingTable()
        assertTrue(table.containsKey("nodeA"))
        assertEquals("neighborB", table["nodeA"]?.nextHopEndpointId)
    }
}
