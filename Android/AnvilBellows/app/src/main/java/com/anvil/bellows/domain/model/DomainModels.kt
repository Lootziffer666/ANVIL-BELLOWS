package com.anvil.bellows.domain.model

data class ChatMessage(
    val id: String,
    val role: String,
    /**
     * Plain-text projection of the message. This remains the canonical value for
     * persistence, memory retrieval, token estimates, and providers that only
     * accept text-only chat payloads.
     */
    val content: String,
    /**
     * Optional multimodal payload for vision-capable models. When present, API
     * adapters should serialize this as the OpenAI-compatible content-parts
     * array instead of downgrading the request to plain text.
     */
    val contentParts: List<ChatContentPart>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val providerId: String? = null,
    val modelId: String? = null,
    val isHandoffBoundary: Boolean = false
) {
    fun hasVisionContent(): Boolean = contentParts.orEmpty().any { it is ChatContentPart.ImageUrl }
}

sealed class ChatContentPart {
    data class Text(val text: String) : ChatContentPart()
    data class ImageUrl(val imageUrl: ChatImageUrl) : ChatContentPart()
}

data class ChatImageUrl(
    val url: String,
    val detail: String? = null
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
    /** Total completed requests in the last 24 hours. */
    val totalRequestsToday: Int = 0,
    /** Total tokens consumed (input + output) in the last 24 hours. */
    val totalTokensToday: Long = 0L,
    val rpmPercent: Float = if (provider.rpmLimit > 0 && provider.rpmLimit < Int.MAX_VALUE)
        rpmUsed.toFloat() / provider.rpmLimit else 0f,
    val rpdPercent: Float = if (provider.rpdLimit > 0 && provider.rpdLimit < Int.MAX_VALUE)
        rpdUsed.toFloat() / provider.rpdLimit else 0f,
    val isAvailable: Boolean
)
