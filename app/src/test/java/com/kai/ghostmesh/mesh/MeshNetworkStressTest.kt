package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class MeshNetworkStressTest {

    @Test
    fun engineHandlesLargeVolumeOfPacketsEfficiently() {
        val engine = MeshEngine(
            myNodeId = "stress-node",
            myNickname = "Stress",
            onSendToNeighbors = { _, _ -> },
            onHandlePacket = {},
            onProfileUpdate = { _ -> }
        )

        val startTime = System.currentTimeMillis()
        val count = 1000
        for (i in 0 until count) {
            val packet = Packet(senderId = "node-$i", senderName = "Node", type = PacketType.CHAT, payload = "Data $i")
            engine.processIncomingJson("endpoint", com.google.gson.Gson().toJson(packet))
        }
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Processing took too long: $duration ms", duration < 2000)
    }
}
