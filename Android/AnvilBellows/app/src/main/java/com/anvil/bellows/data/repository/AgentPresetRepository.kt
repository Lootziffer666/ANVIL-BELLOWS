package com.anvil.bellows.data.repository

import com.anvil.bellows.data.local.db.dao.AgentPresetDao
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentPresetRepository @Inject constructor(
    private val dao: AgentPresetDao
) {
    fun observeAll(): Flow<List<AgentPresetEntity>> = dao.observeAll()
    suspend fun getById(id: String): AgentPresetEntity? = dao.getById(id)
    suspend fun save(preset: AgentPresetEntity) = dao.upsert(preset)
    suspend fun delete(preset: AgentPresetEntity) = dao.delete(preset)
}
