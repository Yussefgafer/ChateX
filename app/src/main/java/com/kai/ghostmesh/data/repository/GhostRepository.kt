package com.kai.ghostmesh.data.repository

import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GhostRepository(
    private val messageDao: MessageDao,
    private val profileDao: ProfileDao
) {
    fun getMessagesForGhost(ghostId: String): Flow<List<Message>> {
        return messageDao.getMessagesForGhost(ghostId).map { entities ->
            entities.map { 
                Message(it.id, it.senderName, it.content, it.isMe, it.isImage, it.isSelfDestruct, it.expiryTime, it.timestamp, it.status, it.hopsTaken) 
            }
        }
    }

    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun saveMessage(packet: Packet, isMe: Boolean, isImage: Boolean, expirySeconds: Int, maxHops: Int) {
        val content = if (isMe) packet.payload else SecurityManager.decrypt(packet.payload)
        val expiryTime = if (packet.isSelfDestruct) System.currentTimeMillis() + (expirySeconds * 1000) else 0
        
        val entity = MessageEntity(
            id = packet.id,
            ghostId = if (isMe) packet.receiverId else packet.senderId,
            senderName = packet.senderName,
            content = content,
            isMe = isMe,
            isImage = isImage,
            isSelfDestruct = packet.isSelfDestruct,
            expiryTime = expiryTime,
            timestamp = packet.timestamp,
            status = if (isMe) MessageStatus.SENT else MessageStatus.DELIVERED,
            hopsTaken = maxHops - packet.hopCount // 🚀 Diagnostic
        )
        messageDao.insertMessage(entity)
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId, status)
    }

    suspend fun syncProfile(profile: ProfileEntity) {
        profileDao.insertProfile(profile)
    }

    suspend fun getProfile(id: String) = profileDao.getProfileById(id)
    suspend fun purgeArchives() = messageDao.clearAllMessages()
    suspend fun burnExpired(currentTime: Long) = messageDao.deleteExpiredMessages(currentTime)
}
