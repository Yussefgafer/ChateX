package com.kai.ghostmesh

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kai.ghostmesh.features.discovery.DiscoveryScreen
import com.kai.ghostmesh.core.model.UserProfile
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class UIPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun auditDiscoveryRecompositions() {
        var recompositionCount = 0
        val nodesState = mutableStateOf(mapOf(
            "1" to UserProfile("1", "Peer 1", transportType = "LAN")
        ))

        composeTestRule.setContent {
            recompositionCount++
            DiscoveryScreen(
                connectedNodes = nodesState.value,
                meshHealth = 100,
                cornerRadius = 12,
                onNodeClick = { _, _ -> },
                onShout = { }
            )
        }

        val initialCount = recompositionCount

        nodesState.value = nodesState.value + ("2" to UserProfile("2", "Peer 2", transportType = "WiFiDirect"))

        composeTestRule.waitForIdle()
        assertTrue("Recomposition should occur on data change", recompositionCount > initialCount)
        composeTestRule.onNodeWithText("Peer 2").assertIsDisplayed()
    }
}
