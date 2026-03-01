package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MeshEngineTest {

    private lateinit var engine: MeshEngine
    private val receivedPackets = mutableListOf<Packet>()
    private val neighborPackets = mutableListOf<Pair<Packet, String?>>()

    @Before
    fun setup() {
        receivedPackets.clear()
        neighborPackets.clear()
        engine = MeshEngine(
            myNodeId = "me",
            myNickname = "MainNode",
            onSendToNeighbors = { packet, exceptId -> neighborPackets.add(packet to exceptId) },
            onHandlePacket = { receivedPackets.add(it) },
            onProfileUpdate = { _ -> }
        )
    }

    @Test
    fun processIncomingJsonDeduplicatesPackets() {
        val packet = Packet(senderId = "other", senderName = "Other", type = PacketType.CHAT, payload = "Hello")
        val json = com.google.gson.Gson().toJson(packet)

        engine.processIncomingJson("link1", json)
        engine.processIncomingJson("link1", json)

        assertEquals(1, receivedPackets.size)
        assertEquals(1, neighborPackets.size)
    }
}
