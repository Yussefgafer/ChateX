package com.kai.ghostmesh.features

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.kai.ghostmesh.base.AppContainer
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyStore

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val application = mockk<GhostApplication>(relaxed = true)
    private val container = mockk<AppContainer>(relaxed = true)
    private val repository = mockk<GhostRepository>(relaxed = true)
    private val meshManager = mockk<MeshManager>(relaxed = true)
    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // Mock KeyStore before SecurityManager initialization
        mockkStatic(KeyStore::class)
        val mockKeyStore = mockk<KeyStore>(relaxed = true)
        every { KeyStore.getInstance("AndroidKeyStore") } returns mockKeyStore

        mockkObject(SecurityManager)
        every { SecurityManager.getMyPublicKey() } returns "test_pub_key"

        every { application.container } returns container
        every { container.repository } returns repository
        every { container.meshManager } returns meshManager
        every { container.myNodeId } returns "test_node_id"

        every { repository.recentChats } returns flowOf(emptyList())
        every { meshManager.incomingPackets } returns MutableSharedFlow<Packet>()
        every { meshManager.connectionUpdates } returns MutableSharedFlow<Map<String, String>>()

        every { application.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor

        // Default values for sharedPrefs
        every { sharedPrefs.getString("nick", any()) } returns "Ghost"
        every { sharedPrefs.getString("status", any()) } returns "Roaming the void"
        every { sharedPrefs.getInt("soul_color", any()) } returns 0xFF00FF7F.toInt()
        every { sharedPrefs.getBoolean("stealth", any()) } returns false
    }

    @Test
    fun testMessagesViewModelInitialization() {
        val viewModel = MessagesViewModel(application)
        assertNotNull(viewModel.recentChats)
    }

    @Test
    fun testMessagesViewModelRefreshConnections() {
        val viewModel = MessagesViewModel(application)
        viewModel.refreshConnections()
        verify { meshManager.stop() }
        verify { meshManager.startMesh(any(), any()) }
    }

    @Test
    fun testChatViewModelInitialization() {
        val viewModel = ChatViewModel(application)
        assertNotNull(viewModel.messages)
    }

    @Test
    fun testChatViewModelSendMessage() {
        val viewModel = ChatViewModel(application)
        val myProfile = UserProfile(id = "test_node_id", name = "Ghost")
        viewModel.setActiveChat("ghost_1")
        viewModel.sendMessage("Hello", false, 0, 3, myProfile)

        verify { meshManager.sendPacket(match { it.payload == "Hello" && it.receiverId == "ghost_1" }) }
        coVerify { repository.saveMessage(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun testDiscoveryViewModelInitialization() {
        val viewModel = DiscoveryViewModel(application)
        assertNotNull(viewModel.connectedNodes)
        assertEquals(0, viewModel.meshHealth.value)
    }

    @Test
    fun testDiscoveryViewModelGlobalShout() {
        val viewModel = DiscoveryViewModel(application)
        val myProfile = UserProfile(id = "test_node_id", name = "Ghost")
        viewModel.globalShout("Shout out!", false, 5, myProfile)

        verify { meshManager.sendPacket(match { it.payload == "Shout out!" && it.receiverId == "ALL" }) }
    }

    @Test
    fun testSettingsViewModelInitialization() {
        val viewModel = SettingsViewModel(application)
        assertNotNull(viewModel.userProfile)
        assertEquals("test_node_id", viewModel.userProfile.value.id)
    }

    @Test
    fun testSettingsViewModelUpdateProfile() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateMyProfile("NewNick", "In the machine")

        assertEquals("NewNick", viewModel.userProfile.value.name)
        verify { editor.putString("nick", "NewNick") }
        verify { meshManager.sendPacket(match { it.type == PacketType.PROFILE_SYNC }) }
    }
}
