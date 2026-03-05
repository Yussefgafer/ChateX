package com.kai.ghostmesh.core.data.local

import androidx.room.*
import com.kai.ghostmesh.core.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    /**
     * Standard Chat Thread: DESC ordering for ReverseLayout compatibility.
     * Newest messages at index 0 (bottom of screen).
     */
    @Query("SELECT * FROM messages WHERE ghostId = :ghostId ORDER BY timestamp ASC")
    fun getMessagesForGhost(ghostId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE ghostId = :ghostId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForGhostPaginated(ghostId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE ghostId = :ghostId")
    suspend fun getMessageCountForGhost(ghostId: String): Int

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("""
        SELECT m1.* FROM messages m1
        INNER JOIN (
            SELECT ghostId, MAX(timestamp) as max_ts
            FROM messages
            GROUP BY ghostId
        ) m2 ON m1.ghostId = m2.ghostId AND m1.timestamp = m2.max_ts
        ORDER BY m1.timestamp DESC
    """)
    fun getRecentMessagesPerGhost(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :newStatus WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: MessageStatus)

    @Query("UPDATE messages SET metadata = :metadata WHERE id = :messageId")
    suspend fun updateMessageMetadata(messageId: String, metadata: String)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("SELECT * FROM messages WHERE metadata LIKE '%\"isSelfDestruct\":true%'")
    suspend fun getSelfDestructMessages(): List<MessageEntity>

    @Delete
    suspend fun deleteMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE expiryTimestamp > 0 AND expiryTimestamp < :currentTime")
    suspend fun getExpiredMessages(currentTime: Long): List<MessageEntity>

    @Query("DELETE FROM messages WHERE expiryTimestamp > 0 AND expiryTimestamp < :currentTime")
    suspend fun deleteExpiredMessages(currentTime: Long)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
