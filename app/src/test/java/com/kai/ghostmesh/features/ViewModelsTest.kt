package com.kai.ghostmesh.features

import android.app.Application
import com.kai.ghostmesh.base.AppContainer
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.features.discovery.DiscoveryViewModel
import com.kai.ghostmesh.features.messages.MessagesViewModel
import com.kai.ghostmesh.features.settings.SettingsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val application = mockk<GhostApplication>(relaxed = true)
    private val container = mockk<AppContainer>(relaxed = true)
    private val repository = mockk<GhostRepository>(relaxed = true)
    private val meshManager = mockk<MeshManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { application.container } returns container
        every { container.repository } returns repository
        every { container.meshManager } returns meshManager
        every { repository.recentChats } returns flowOf(emptyList())
        every { meshManager.incomingPackets } returns MutableSharedFlow<Packet>()
        every { meshManager.connectionUpdates } returns MutableSharedFlow<Map<String, String>>()
    }

    @Test
    fun testMessagesViewModelInitialization() {
        val viewModel = MessagesViewModel(application)
        assertNotNull(viewModel.recentChats)
    }

    @Test
    fun testChatViewModelInitialization() {
        val viewModel = ChatViewModel(application)
        assertNotNull(viewModel.messages)
    }

    @Test
    fun testDiscoveryViewModelInitialization() {
        val viewModel = DiscoveryViewModel(application)
        assertNotNull(viewModel.connectedNodes)
    }

    @Test
    fun testSettingsViewModelInitialization() {
        val viewModel = SettingsViewModel(application)
        assertNotNull(viewModel.userProfile)
    }
}
