package com.anvil.bellows.data.repository

import com.anvil.bellows.data.local.db.dao.ConversationMessageDao
import com.anvil.bellows.data.local.db.dao.ConversationSessionDao
import com.anvil.bellows.data.local.db.entity.ConversationMessageEntity
import com.anvil.bellows.data.local.db.entity.ConversationSessionEntity
import com.anvil.bellows.domain.model.ChatMessage
import com.anvil.bellows.domain.model.ConversationSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val sessionDao: ConversationSessionDao,
    private val messageDao: ConversationMessageDao
) {
    fun observeSessions(): Flow<List<ConversationSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeSessionsByProject(projectId: String): Flow<List<ConversationSession>> =
        sessionDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.observeBySession(sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun getMessages(sessionId: String): List<ChatMessage> =
        messageDao.getBySession(sessionId).map { it.toDomain() }

    suspend fun getSession(sessionId: String): ConversationSession? =
        sessionDao.getById(sessionId)?.toDomain()

    suspend fun createSession(
        projectId: String?,
        providerId: String,
        modelId: String,
        presetId: String? = null,
        systemPrompt: String = ""
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        sessionDao.upsert(
            ConversationSessionEntity(
                id = id,
                projectId = projectId,
                title = "New Chat",
                createdAt = now,
                updatedAt = now,
                currentProviderId = providerId,
                currentModelId = modelId,
                systemPrompt = systemPrompt,
                presetId = presetId
            )
        )
        return id
    }

    suspend fun saveMessage(
        sessionId: String,
        role: String,
        content: String,
        providerId: String? = null,
        modelId: String? = null,
        isHandoffBoundary: Boolean = false
    ): String {
        val id = UUID.randomUUID().toString()
        messageDao.insert(
            ConversationMessageEntity(
                id = id,
                sessionId = sessionId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
                providerId = providerId,
                modelId = modelId,
                isHandoffBoundary = isHandoffBoundary,
                tokenCount = estimateTokens(content)
            )
        )
        val title = if (role == "user") content.take(50) else null
        sessionDao.getById(sessionId)?.let { session ->
            sessionDao.update(
                session.copy(
                    title = if (title != null && session.title == "New Chat") title else session.title,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return id
    }

    suspend fun deleteSession(sessionId: String) {
        messageDao.deleteBySession(sessionId)
        sessionDao.deleteById(sessionId)
    }

    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private fun ConversationSessionEntity.toDomain() = ConversationSession(
        id = id, projectId = projectId, title = title,
        createdAt = createdAt, updatedAt = updatedAt,
        currentProviderId = currentProviderId, handoffCount = handoffCount
    )

    private fun ConversationMessageEntity.toDomain() = ChatMessage(
        id = id, role = role, content = content, timestamp = timestamp,
        providerId = providerId, modelId = modelId, isHandoffBoundary = isHandoffBoundary
    )
}
