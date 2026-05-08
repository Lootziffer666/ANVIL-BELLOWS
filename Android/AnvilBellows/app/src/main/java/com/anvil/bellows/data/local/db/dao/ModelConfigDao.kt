package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Query("SELECT * FROM model_configs WHERE providerId = :providerId")
    fun observeByProvider(providerId: String): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId")
    suspend fun getByProvider(providerId: String): List<ModelConfigEntity>

    @Upsert
    suspend fun upsertAll(entities: List<ModelConfigEntity>)

    @Delete
    suspend fun delete(entity: ModelConfigEntity)
}
