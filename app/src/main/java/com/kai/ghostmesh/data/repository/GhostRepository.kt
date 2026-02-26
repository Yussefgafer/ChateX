package com.kai.ghostmesh.data.repository

import com.google.gson.Gson
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GhostRepository(
    private val messageDao: MessageDao,
    private val profileDao: ProfileDao
) {
    private val gson = Gson()
    private val GLOBAL_VOID_ID = "ALL" // Virtual ID for Broadcasts

    fun getMessagesForGhost(ghostId: String): Flow<List<Message>> {
        return messageDao.getMessagesForGhost(ghostId).map { entities ->
            entities.map { entity ->
                @Suppress("UNCHECKED_CAST")
                val meta = try { gson.fromJson(entity.metadata, Map::class.java) as Map<String, Any> } catch (e: Exception) { emptyMap() }
                Message(
                    id = entity.id, sender = entity.senderName, content = entity.content, isMe = entity.isMe,
                    isImage = meta["isImage"] as? Boolean ?: false,
                    isVoice = meta["isVoice"] as? Boolean ?: false,
                    isSelfDestruct = meta["isSelfDestruct"] as? Boolean ?: false,
                    expiryTime = (meta["expiryTime"] as? Double)?.toLong() ?: 0L,
                    timestamp = entity.timestamp, status = entity.status,
                    hopsTaken = (meta["hops"] as? Double)?.toInt() ?: 0
                )
            }
        }
    }

    val recentChats: Flow<List<RecentChat>> = combine(
        profileDao.getAllProfiles(),
        messageDao.getAllMessages()
    ) { profiles, messages ->
        val chats = profiles.map { profileEntity ->
            val lastMsg = messages.firstOrNull { it.ghostId == profileEntity.id }
            RecentChat(
                profile = UserProfile(profileEntity.id, profileEntity.name, profileEntity.status, profileEntity.color),
                lastMessage = when {
                    lastMsg?.metadata?.contains("\"isImage\":true") == true -> "Spectral Image"
                    lastMsg?.metadata?.contains("\"isVoice\":true") == true -> "Spectral Voice"
                    else -> lastMsg?.content ?: "No messages yet"
                },
                lastMessageTime = lastMsg?.timestamp ?: profileEntity.lastSeen
            )
        }.toMutableList()

        // Add the "Global Void" if there are any broadcast messages
        val lastGlobalMsg = messages.firstOrNull { it.ghostId == GLOBAL_VOID_ID }
        if (lastGlobalMsg != null) {
            chats.add(RecentChat(
                profile = UserProfile(GLOBAL_VOID_ID, "GLOBAL VOID", "The public spectral channel", 0xFFBB86FC.toInt()),
                lastMessage = lastGlobalMsg.content,
                lastMessageTime = lastGlobalMsg.timestamp
            ))
        }

        chats.sortedByDescending { it.lastMessageTime }
    }

    suspend fun saveMessage(packet: Packet, isMe: Boolean, isImage: Boolean, isVoice: Boolean, expirySeconds: Int, maxHops: Int) {
        val content = if (isMe) packet.payload else SecurityManager.decrypt(packet.payload)
        val expiryTime = if (packet.isSelfDestruct) System.currentTimeMillis() + (expirySeconds * 1000) else 0L
        
        // If it's a broadcast from me, save it under GLOBAL_VOID_ID so I can see it
        val targetId = if (packet.receiverId == "ALL") "ALL" else if (isMe) packet.receiverId else packet.senderId

        val meta = mapOf("isImage" to isImage, "isVoice" to isVoice, "isSelfDestruct" to packet.isSelfDestruct, "expiryTime" to expiryTime, "hops" to (maxHops - packet.hopCount))
        messageDao.insertMessage(MessageEntity(
            id = packet.id, ghostId = targetId,
            senderName = packet.senderName, content = content, isMe = isMe,
            timestamp = packet.timestamp, status = if (isMe) MessageStatus.SENT else MessageStatus.DELIVERED,
            metadata = gson.toJson(meta)
        ))
    }

    suspend fun deleteMessage(id: String) = messageDao.deleteMessageById(id)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) = messageDao.updateMessageStatus(messageId, status)
    suspend fun syncProfile(profile: ProfileEntity) = profileDao.insertProfile(profile)
    suspend fun getProfile(id: String) = profileDao.getProfileById(id)
    suspend fun purgeArchives() = messageDao.clearAllMessages()
    suspend fun burnExpired(currentTime: Long) {
        val candidates = messageDao.getSelfDestructMessages()
        val toDelete = candidates.filter { entity ->
            @Suppress("UNCHECKED_CAST")
            val meta = try { gson.fromJson(entity.metadata, Map::class.java) as Map<String, Any> } catch (e: Exception) { emptyMap() }
            val expiryTime = (meta["expiryTime"] as? Double)?.toLong() ?: 0L
            expiryTime > 0 && expiryTime < currentTime
        }
        if (toDelete.isNotEmpty()) messageDao.deleteMessages(toDelete)
    }
}
