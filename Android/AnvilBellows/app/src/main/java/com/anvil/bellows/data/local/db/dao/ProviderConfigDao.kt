package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderConfigDao {
    @Query("SELECT * FROM provider_configs ORDER BY tier ASC, name ASC")
    fun observeAll(): Flow<List<ProviderConfigEntity>>

    @Query("SELECT * FROM provider_configs WHERE enabled = 1 ORDER BY tier ASC, rpmLimit DESC")
    suspend fun getEnabledProviders(): List<ProviderConfigEntity>

    @Query("SELECT * FROM provider_configs WHERE id = :id")
    suspend fun getById(id: String): ProviderConfigEntity?

    @Query("SELECT * FROM provider_configs WHERE isByok = 1 ORDER BY tier ASC")
    fun observeByok(): Flow<List<ProviderConfigEntity>>

    @Upsert
    suspend fun upsert(entity: ProviderConfigEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ProviderConfigEntity>)

    @Update
    suspend fun update(entity: ProviderConfigEntity)

    @Delete
    suspend fun delete(entity: ProviderConfigEntity)

    @Query("DELETE FROM provider_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM provider_configs")
    suspend fun count(): Int
}
