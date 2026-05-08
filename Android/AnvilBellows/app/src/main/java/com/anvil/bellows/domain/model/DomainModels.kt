package com.anvil.bellows.domain.model

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val providerId: String? = null,
    val modelId: String? = null,
    val isHandoffBoundary: Boolean = false
)

data class Provider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val rpmLimit: Int,
    val rpdLimit: Int,
    val contextWindow: Int,
    val maxOutput: Int,
    val tier: Int,
    val isByok: Boolean,
    val enabled: Boolean,
    val selectedModel: String,
    val authType: String = "API_KEY",
    val vertexProjectId: String? = null,
    val vertexLocation: String? = null,
    val rpmUsed: Int = 0,
    val rpdUsed: Int = 0
)

data class ConversationSession(
    val id: String,
    val projectId: String?,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val currentProviderId: String,
    val handoffCount: Int
)

data class Project(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val parentId: String?,
    val createdAt: Long,
    val children: List<Project> = emptyList(),
    val sessions: List<ConversationSession> = emptyList()
)

data class MemoryChunk(
    val id: String,
    val sessionId: String,
    val projectId: String?,
    val content: String,
    val tags: List<String>,
    val createdAt: Long
)

data class HandoffEvent(
    val fromProviderId: String,
    val fromProviderName: String,
    val toProviderId: String,
    val toProviderName: String,
    val reason: String
)

data class QuotaStatus(
    val provider: Provider,
    val rpmUsed: Int,
    val rpdUsed: Int,
    val rpmPercent: Float = if (provider.rpmLimit > 0) rpmUsed.toFloat() / provider.rpmLimit else 0f,
    val rpdPercent: Float = if (provider.rpdLimit < Int.MAX_VALUE) rpdUsed.toFloat() / provider.rpdLimit else 0f,
    val isAvailable: Boolean
)
