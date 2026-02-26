package com.kai.ghostmesh.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE ghostId = :ghostId ORDER BY timestamp ASC")
    fun getMessagesForGhost(ghostId: String): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE isSelfDestruct = 1 AND expiryTime < :currentTime")
    suspend fun deleteExpiredMessages(currentTime: Long) // 🚀 Burn!

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
