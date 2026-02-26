package com.kai.ghostmesh.data.local

import androidx.room.*
import com.kai.ghostmesh.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE ghostId = :ghostId ORDER BY timestamp ASC")
    fun getMessagesForGhost(ghostId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :newStatus WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: MessageStatus)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String) // New!

    @Query("SELECT * FROM messages WHERE metadata LIKE '%\"isSelfDestruct\":true%'")
    suspend fun getSelfDestructMessages(): List<MessageEntity>

    @Delete
    suspend fun deleteMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
