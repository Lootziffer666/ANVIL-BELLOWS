package com.anvil.bellows.data.repository

import com.anvil.bellows.data.local.db.dao.ProjectDao
import com.anvil.bellows.data.local.db.entity.ProjectEntity
import com.anvil.bellows.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    fun observeAll(): Flow<List<Project>> =
        projectDao.observeAll().map { list ->
            buildTree(list.map { it.toDomain() })
        }

    suspend fun createProject(name: String, color: String, icon: String, parentId: String?): String {
        val id = UUID.randomUUID().toString()
        projectDao.upsert(
            ProjectEntity(
                id = id, name = name, color = color, icon = icon,
                parentId = parentId, createdAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun deleteProject(projectId: String) = projectDao.deleteById(projectId)

    private fun buildTree(flat: List<Project>): List<Project> {
        val byParent = flat.groupBy { it.parentId }
        fun buildNode(project: Project): Project =
            project.copy(children = (byParent[project.id] ?: emptyList()).map { buildNode(it) })
        return (byParent[null] ?: emptyList()).map { buildNode(it) }
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id, name = name, color = color, icon = icon,
        parentId = parentId, createdAt = createdAt
    )
}
