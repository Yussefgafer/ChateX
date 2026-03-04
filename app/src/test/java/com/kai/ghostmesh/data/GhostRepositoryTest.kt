package com.kai.ghostmesh.data

import com.kai.ghostmesh.core.data.local.*
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.model.*
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GhostRepositoryTest {

    private lateinit var repository: GhostRepository
    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val profileDao = mockk<ProfileDao>(relaxed = true)

    @Before
    fun setup() {
        repository = GhostRepository(messageDao, profileDao)
    }

    @Test
    fun getMessagesForGhostReturnsMappedMessages() = runTest {
        val ghostId = "test-ghost"
        val entities = listOf(
            MessageEntity(id = "1", ghostId = ghostId, senderName = "Ghost", content = "Hi", isMe = false, timestamp = 1000L, status = MessageStatus.DELIVERED, metadata = "{}")
        )

        // Mock all potential flows to avoid any hangs in Flow.combine or initializations
        every { messageDao.getMessagesForGhost(any()) } returns flowOf(entities)
        every { messageDao.getRecentMessagesPerGhost() } returns flowOf(emptyList())
        every { profileDao.getAllProfiles() } returns flowOf(emptyList())

        // Use withTimeout to ensure the test fails rather than hangs if Flow doesn't emit
        val result = withTimeout(5000) {
            repository.getMessagesForGhost(ghostId).first()
        }

        assertNotNull("Result should not be null", result)
        assertEquals(1, result.size)
        assertEquals("Hi", result[0].content)
        assertEquals("Ghost", result[0].sender)
    }

    @Test
    fun saveMessageInsertsIntoDao() = runTest {
        val packet = Packet(senderId = "sender", senderName = "Sender", receiverId = "receiver", type = PacketType.CHAT, payload = "Hello")

        // Mock all potential flows to avoid any hangs
        every { messageDao.getRecentMessagesPerGhost() } returns flowOf(emptyList())
        every { profileDao.getAllProfiles() } returns flowOf(emptyList())

        repository.saveMessage(packet, isMe = true, isImage = false, isVoice = false, isVideo = false, expirySeconds = 0, maxHops = 3)
        coVerify { messageDao.insertMessage(any()) }
    }
}
