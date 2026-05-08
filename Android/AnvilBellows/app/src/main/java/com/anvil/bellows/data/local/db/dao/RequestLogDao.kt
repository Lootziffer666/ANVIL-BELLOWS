package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.RequestLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestLogDao {
    @Insert
    suspend fun insert(log: RequestLogEntity): Long

    @Query("SELECT COUNT(*) FROM request_logs WHERE providerId = :providerId AND timestamp > :since")
    suspend fun countSince(providerId: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM request_logs WHERE providerId = :providerId AND timestamp > :since")
    fun observeCountSince(providerId: String, since: Long): Flow<Int>

    @Query("SELECT SUM(inputTokens + outputTokens) FROM request_logs WHERE providerId = :providerId AND timestamp > :since")
    suspend fun sumTokensSince(providerId: String, since: Long): Long?

    @Query("SELECT * FROM request_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: String): List<RequestLogEntity>

    @Query("DELETE FROM request_logs WHERE timestamp < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long)

    @Query("SELECT * FROM request_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeRecent(): Flow<List<RequestLogEntity>>
}
