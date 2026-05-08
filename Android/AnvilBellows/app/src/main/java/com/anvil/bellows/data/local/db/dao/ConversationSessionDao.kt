package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.ConversationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationSessionDao {
    @Query("SELECT * FROM conversation_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationSessionEntity>>

    @Query("SELECT * FROM conversation_sessions WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeByProject(projectId: String): Flow<List<ConversationSessionEntity>>

    @Query("SELECT * FROM conversation_sessions WHERE projectId IS NULL ORDER BY updatedAt DESC")
    fun observeUnorganized(): Flow<List<ConversationSessionEntity>>

    @Query("SELECT * FROM conversation_sessions WHERE id = :id")
    suspend fun getById(id: String): ConversationSessionEntity?

    @Upsert
    suspend fun upsert(entity: ConversationSessionEntity)

    @Update
    suspend fun update(entity: ConversationSessionEntity)

    @Delete
    suspend fun delete(entity: ConversationSessionEntity)

    @Query("DELETE FROM conversation_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
