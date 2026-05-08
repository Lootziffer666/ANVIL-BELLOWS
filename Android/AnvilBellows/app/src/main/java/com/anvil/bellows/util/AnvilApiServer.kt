package com.anvil.bellows.util

import com.anvil.bellows.data.local.db.DatabaseInitializer
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.data.repository.ConversationRepository
import com.anvil.bellows.domain.model.ChatMessage
import com.anvil.bellows.domain.usecase.ChatUseCase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnvilApiServer @Inject constructor(
    private val chatUseCase: ChatUseCase,
    private val conversationRepository: ConversationRepository,
    private val providerConfigDao: ProviderConfigDao,
    private val databaseInitializer: DatabaseInitializer,
    private val encryptedPrefs: EncryptedPrefsManager,
    private val gson: Gson
) : NanoHTTPD("127.0.0.1", 4141) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun serve(session: IHTTPSession): Response {
        val token = encryptedPrefs.getOrCreateLocalApiToken()
        val authHeader = session.headers["authorization"] ?: ""
        if (authHeader != "Bearer $token") {
            return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED, "application/json",
                """{"error":{"message":"Unauthorized","type":"auth_error"}}"""
            )
        }

        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "")
        }
        if (session.uri != "/v1/chat/completions") {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "application/json",
                """{"error":{"message":"Only POST /v1/chat/completions is supported","type":"not_found"}}"""
            )
        }

        return try {
            val body = HashMap<String, String>()
            session.parseBody(body)
            val postData = body["postData"] ?: body["content"] ?: ""
            val request = gson.fromJson(postData, OpenAiRequest::class.java)
                ?: return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, "application/json",
                    """{"error":{"message":"Invalid JSON body","type":"bad_request"}}"""
                )

            val messages = request.messages.map {
                ChatMessage(id = UUID.randomUUID().toString(), role = it.role, content = it.content)
            }

            if (request.stream == true) streamResponse(messages) else blockingResponse(messages)
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "application/json",
                gson.toJson(mapOf("error" to mapOf("message" to e.message, "type" to "internal_error")))
            )
        }
    }

    private suspend fun resolveSessionId(): String {
        databaseInitializer.ready.await()
        val provider = providerConfigDao.getEnabledProviders().firstOrNull()
            ?: throw IllegalStateException("No enabled provider")
        return conversationRepository.createSession(
            projectId = null,
            providerId = provider.id,
            modelId = provider.selectedModel
        )
    }

    private fun streamResponse(messages: List<ChatMessage>): Response {
        val pipeOut = PipedOutputStream()
        val pipeIn = PipedInputStream(pipeOut, 8192)
        val writer = pipeOut.bufferedWriter()

        scope.launch {
            try {
                val sessionId = resolveSessionId()
                chatUseCase.chat(messages, sessionId).collect { result ->
                    when (result) {
                        is ChatUseCase.ChatResult.Token -> {
                            writer.write(buildSseChunk(result.text))
                            writer.flush()
                        }
                        is ChatUseCase.ChatResult.Complete -> {
                            writer.write("data: [DONE]\n\n")
                            writer.flush()
                        }
                        is ChatUseCase.ChatResult.Error -> {
                            val payload = gson.toJson(
                                mapOf("error" to mapOf("message" to result.message, "type" to "provider_error"))
                            )
                            writer.write("data: $payload\n\n")
                            writer.flush()
                        }
                        else -> {}
                    }
                }
            } finally {
                runCatching { writer.close() }
            }
        }

        return newChunkedResponse(Response.Status.OK, "text/event-stream", pipeIn)
    }

    private fun blockingResponse(messages: List<ChatMessage>): Response {
        val accumulated = StringBuilder()
        var modelId = "anvil-bellows"

        runBlocking {
            val sessionId = resolveSessionId()
            chatUseCase.chat(messages, sessionId).collect { result ->
                when (result) {
                    is ChatUseCase.ChatResult.Token -> {
                        accumulated.append(result.text)
                        modelId = result.providerId
                    }
                    else -> {}
                }
            }
        }

        val json = gson.toJson(mapOf(
            "id" to "catg-${System.currentTimeMillis()}",
            "object" to "chat.completion",
            "model" to modelId,
            "choices" to listOf(mapOf(
                "index" to 0,
                "message" to mapOf("role" to "assistant", "content" to accumulated.toString()),
                "finish_reason" to "stop"
            )),
            "usage" to mapOf(
                "prompt_tokens" to 0,
                "completion_tokens" to (accumulated.length / 4),
                "total_tokens" to (accumulated.length / 4)
            )
        ))
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun buildSseChunk(token: String): String {
        val payload = gson.toJson(mapOf(
            "id" to "catg",
            "object" to "chat.completion.chunk",
            "choices" to listOf(mapOf(
                "delta" to mapOf("content" to token),
                "index" to 0,
                "finish_reason" to null
            ))
        ))
        return "data: $payload\n\n"
    }

    fun startServer() {
        if (!isAlive) start(SOCKET_READ_TIMEOUT, false)
    }

    fun stopServer() {
        if (isAlive) stop()
        scope.cancel()
    }

    data class OpenAiRequest(
        val model: String = "",
        val messages: List<MsgDto> = emptyList(),
        val stream: Boolean? = null,
        @SerializedName("max_tokens") val maxTokens: Int? = null
    )

    data class MsgDto(val role: String = "user", val content: String = "")
}
