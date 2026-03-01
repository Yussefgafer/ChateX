package com.kai.ghostmesh

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testInitialScreenIsMessages() {
        // Check for MESH HUB title
        composeTestRule.onNodeWithText("MESH HUB", substring = true).assertExists()
    }

    @Test
    fun testNavigateToSettings() {
        // Click FAB with content description "Settings"
        composeTestRule.onNodeWithContentDescription("Settings", ignoreCase = true).performClick()

        // Settings screen should contain "Profile"
        composeTestRule.onNodeWithText("Profile", substring = true).assertExists()
    }

    @Test
    fun testNavigateToRadar() {
        // Click FAB with content description "Radar"
        composeTestRule.onNodeWithContentDescription("Radar", ignoreCase = true).performClick()

        // Discovery screen should have "MESH INTEGRITY" or "Nodes"
        composeTestRule.onNodeWithText("MESH INTEGRITY", substring = true).assertExists()
    }
}
