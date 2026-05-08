package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.HandoffLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HandoffLogDao {
    @Insert
    suspend fun insert(entity: HandoffLogEntity): Long

    @Query("SELECT * FROM handoff_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<HandoffLogEntity>>

    @Query("SELECT COUNT(*) FROM handoff_logs WHERE timestamp > :since")
    suspend fun countSince(since: Long): Int
}
