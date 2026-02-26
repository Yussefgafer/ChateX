package com.kai.ghostmesh.data.local

import androidx.room.*
import com.kai.ghostmesh.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE ghostId = :ghostId ORDER BY timestamp ASC")
    fun getMessagesForGhost(ghostId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 🚀 Important for ACKs
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :newStatus WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: MessageStatus)

    @Query("DELETE FROM messages WHERE isSelfDestruct = 1 AND expiryTime < :currentTime")
    suspend fun deleteExpiredMessages(currentTime: Long)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
