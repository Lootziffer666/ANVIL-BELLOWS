package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentPresetDao {
    @Query("SELECT * FROM agent_presets ORDER BY isPinned DESC, name ASC")
    fun observeAll(): Flow<List<AgentPresetEntity>>

    @Query("SELECT * FROM agent_presets WHERE id = :id")
    suspend fun getById(id: String): AgentPresetEntity?

    @Upsert
    suspend fun upsert(preset: AgentPresetEntity)

    @Delete
    suspend fun delete(preset: AgentPresetEntity)

    @Query("SELECT COUNT(*) FROM agent_presets")
    suspend fun count(): Int
}
