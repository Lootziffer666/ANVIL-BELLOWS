package com.anvil.bellows.util

import com.anvil.bellows.data.local.db.dao.HandoffLogDao
import com.anvil.bellows.data.local.db.dao.ConversationSessionDao
import com.anvil.bellows.data.local.db.entity.HandoffLogEntity
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandoffManager @Inject constructor(
    private val rateLimitTracker: RateLimitTracker,
    private val handoffLogDao: HandoffLogDao,
    private val conversationSessionDao: ConversationSessionDao
) {
    suspend fun selectNextProvider(
        currentProviderId: String,
        excludeIds: Set<String>
    ): ProviderConfigEntity? =
        rateLimitTracker.getRankedAvailableProviders()
            .firstOrNull { it.id != currentProviderId && it.id !in excludeIds }

    suspend fun logHandoff(
        sessionId: String,
        fromProviderId: String,
        toProviderId: String,
        reason: String,
        messageCount: Int
    ) {
        handoffLogDao.insert(
            HandoffLogEntity(
                sessionId = sessionId,
                fromProviderId = fromProviderId,
                toProviderId = toProviderId,
                reason = reason,
                timestamp = System.currentTimeMillis(),
                messageCountAtHandoff = messageCount
            )
        )
        conversationSessionDao.getById(sessionId)?.let { session ->
            conversationSessionDao.update(
                session.copy(
                    currentProviderId = toProviderId,
                    handoffCount = session.handoffCount + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
