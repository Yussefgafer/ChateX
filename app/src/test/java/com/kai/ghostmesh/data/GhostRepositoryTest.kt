package com.kai.ghostmesh.data

import com.kai.ghostmesh.core.data.local.*
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.model.*
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GhostRepositoryTest {

    private lateinit var repository: GhostRepository
    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val profileDao = mockk<ProfileDao>(relaxed = true)

    @Before
    fun setup() {
        repository = GhostRepository(messageDao, profileDao)
    }

    @Test
    fun getMessagesForGhostReturnsMappedMessages() = runBlocking {
        val ghostId = "test-ghost"
        val entities = listOf(
            MessageEntity(id = "1", ghostId = ghostId, senderName = "Ghost", content = "Hi", isMe = false, timestamp = 1000L, status = MessageStatus.DELIVERED, metadata = "{}")
        )
        every { messageDao.getMessagesForGhost(ghostId) } returns flowOf(entities)

        val result = repository.getMessagesForGhost(ghostId).first()

        assertEquals(1, result.size)
        assertEquals("Hi", result[0].content)
        assertEquals("Ghost", result[0].sender)
    }

    @Test
    fun saveMessageInsertsIntoDao() = runBlocking {
        val packet = Packet(senderId = "sender", senderName = "Sender", receiverId = "receiver", type = PacketType.CHAT, payload = "Hello")
        repository.saveMessage(packet, isMe = true, isImage = false, isVoice = false, expirySeconds = 0, maxHops = 3)
        coVerify { messageDao.insertMessage(any()) }
    }
}
