package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.MemoryChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryChunkDao {
    @Insert
    suspend fun insert(entity: MemoryChunkEntity): Long

    @Query("SELECT * FROM memory_chunks WHERE projectId = :projectId ORDER BY createdAt DESC")
    suspend fun getByProject(projectId: String): List<MemoryChunkEntity>

    @Query("SELECT * FROM memory_chunks ORDER BY createdAt DESC")
    suspend fun getAll(): List<MemoryChunkEntity>

    @Query("SELECT * FROM memory_chunks WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeBySession(sessionId: String): Flow<List<MemoryChunkEntity>>

    @Delete
    suspend fun delete(entity: MemoryChunkEntity)

    @Query("DELETE FROM memory_chunks WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
