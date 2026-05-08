package com.anvil.bellows.data.local.db.dao

import androidx.room.*
import com.anvil.bellows.data.local.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeRootProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun observeChildren(parentId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Upsert
    suspend fun upsert(entity: ProjectEntity)

    @Delete
    suspend fun delete(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
