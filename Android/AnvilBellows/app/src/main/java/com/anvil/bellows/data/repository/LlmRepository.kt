package com.anvil.bellows.data.repository

import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.data.remote.api.LlmApiService
import com.anvil.bellows.data.remote.dto.ChatRequest
import com.anvil.bellows.data.remote.dto.MessageDto
import com.anvil.bellows.domain.model.ChatMessage
import com.anvil.bellows.util.RateLimitTracker
import com.anvil.bellows.util.SseStreamParser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepository @Inject constructor(
    private val llmApiService: LlmApiService,
    private val encryptedPrefs: EncryptedPrefsManager,
    private val rateLimitTracker: RateLimitTracker,
    private val gson: Gson
) {
    fun streamChat(
        provider: ProviderConfigEntity,
        messages: List<ChatMessage>
    ): Flow<String> = flow {
        // ── Auth resolution ────────────────────────────────────────────────────
        // noAuth providers (e.g. Pollinations) require no key; passing "" causes
        // DynamicBaseUrlInterceptor to strip the Authorization header entirely.
        val authorization = if (provider.noAuth) {
            ""
        } else {
            val apiKey = encryptedPrefs.getApiKey(provider.id)
                ?: throw IllegalStateException("No API key for ${provider.name}")
            buildAuthHeader(provider, apiKey)
        }

        val request = ChatRequest(
            model = provider.selectedModel,
            messages = messages.map { MessageDto(it.role, it.content) },
            stream = true,
            maxTokens = provider.maxOutput.coerceAtMost(8192)
        )

        val useZaiSemaphore = provider.id == "zai"
        if (useZaiSemaphore) rateLimitTracker.getZaiSemaphore().acquire()

        try {
            val call = llmApiService.chatCompletionStream(
                baseUrl      = provider.baseUrl,
                authorization = authorization,
                request      = request
            )
            val response = call.execute()
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val body = response.body()
                ?: throw IOException("Empty response body from ${provider.name}")
            SseStreamParser.parseStream(body, gson).collect { emit(it) }
        } finally {
            if (useZaiSemaphore) rateLimitTracker.getZaiSemaphore().release()
        }
    }.flowOn(Dispatchers.IO)

    // ── Auth header construction ────────────────────────────────────────────────
    // Anthropic uses x-api-key header (handled by authHeaderName in the entity),
    // but the LlmApiService always sends the value as "Authorization". Providers
    // that deviate from standard Bearer auth should instead use a custom
    // authHeaderName configured in ProviderRegistry and respected by the server.
    private fun buildAuthHeader(provider: ProviderConfigEntity, apiKey: String): String =
        when (provider.authType) {
            "VERTEX"  -> "Bearer $apiKey"
            "API_KEY" -> if (provider.authHeaderName == "Authorization") "Bearer $apiKey" else apiKey
            else      -> "Bearer $apiKey"
        }
}
