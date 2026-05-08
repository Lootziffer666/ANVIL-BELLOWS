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
        val apiKey = encryptedPrefs.getApiKey(provider.id)
            ?: throw IllegalStateException("No API key for ${provider.name}")

        val authorization = buildAuthHeader(provider, apiKey)
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
                baseUrl = provider.baseUrl,
                authorization = authorization,
                request = request
            )
            val response = call.execute()
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val body = response.body() ?: throw IOException("Empty response body from ${provider.name}")
            SseStreamParser.parseStream(body, gson).collect { emit(it) }
        } finally {
            if (useZaiSemaphore) rateLimitTracker.getZaiSemaphore().release()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildAuthHeader(provider: ProviderConfigEntity, apiKey: String): String =
        when (provider.authType) {
            "VERTEX" -> "Bearer $apiKey"
            else -> "Bearer $apiKey"
        }
}
