package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.ConversationMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<ConversationMessageEntity>>

    @Query("SELECT * FROM conversation_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: String): List<ConversationMessageEntity>

    @Insert
    suspend fun insert(entity: ConversationMessageEntity): Long

    @Upsert
    suspend fun upsert(entity: ConversationMessageEntity)

    @Delete
    suspend fun delete(entity: ConversationMessageEntity)

    @Query("DELETE FROM conversation_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("SELECT COUNT(*) FROM conversation_messages WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: String): Int
}
