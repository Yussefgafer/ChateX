package com.kai.ghostmesh.mesh

import org.junit.Ignore

import android.content.Context
import android.content.SharedPreferences
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.Constants
import io.mockk.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals

class MeshManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)

    @Test
    fun testInitialization() {
        every { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
        val meshManager = MeshManager(context, "test-node-id")
        assert(meshManager is MeshManager)
    }

    @Test
    fun testPacketStatisticsAtomic() = runTest {
        val meshManager = MeshManager(context, "test-node-id")

        // Simulate concurrent updates
        repeat(100) {
            meshManager.totalPacketsSent.value += 1
        }
        assertEquals(100, meshManager.totalPacketsSent.value)
    }
}
