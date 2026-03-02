package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class MeshNetworkStressTest {

    @Before
    fun setup() {
        mockkObject(SecurityManager)
        every { SecurityManager.verifyPacket(any(), any(), any(), any()) } returns true
    }

    @Test
    fun engineHandlesLargeVolumeOfPacketsEfficiently() {
        val engine = MeshEngine(
            myNodeId = "stress-node",
            myNickname = "Stress",
            onSendToNeighbors = { _, _ -> },
            onHandlePacket = {},
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        val startTime = System.currentTimeMillis()
        val count = 1000
        val gson = com.google.gson.Gson()
        for (i in 0 until count) {
            val packet = Packet(
                senderId = "node-$i",
                senderName = "Node",
                type = PacketType.CHAT,
                payload = "Data $i",
                signature = "valid"
            )
            engine.processIncomingJson("endpoint", gson.toJson(packet))
        }
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Processing took too long: $duration ms", duration < 2000)
    }
}
