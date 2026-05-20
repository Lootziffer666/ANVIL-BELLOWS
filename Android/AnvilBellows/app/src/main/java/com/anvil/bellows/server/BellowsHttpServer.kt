package com.anvil.bellows.server

import android.util.Log
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.data.repository.LlmRepository
import com.anvil.bellows.domain.model.ChatMessage
import com.anvil.bellows.util.RateLimitTracker
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BellowsHttpServer"

/**
 * OpenAI-compatible local HTTP server running on port [PORT].
 *
 * Security model
 * ─────────────
 * Every request must carry a valid `Authorization: Bearer <token>` header.
 * The token is compared (constant-time string equality) against the value
 * stored in [EncryptedPrefsManager.KEY_LOCAL_API_TOKEN]. Requests without
 * a matching token receive HTTP 401.
 *
 * Supported endpoints
 * ───────────────────
 *  POST /v1/chat/completions  – streaming (SSE) and non-streaming chat
 *  GET  /v1/models            – list of enabled provider models
 *  GET  /health               – simple liveness probe (no auth required)
 *
 * Provider routing
 * ────────────────
 * The best available provider is selected via [RateLimitTracker.getRankedAvailableProviders].
 * Callers can override by supplying `X-Bellows-Provider-Id` in the request header.
 */
@Singleton
class BellowsHttpServer @Inject constructor(
    private val encryptedPrefs: EncryptedPrefsManager,
    private val rateLimitTracker: RateLimitTracker,
    private val llmRepository: LlmRepository,
    private val providerConfigDao: ProviderConfigDao,
    private val gson: Gson
) : NanoHTTPD(PORT) {

    companion object {
        const val PORT = 4141
        private const val HEADER_PROVIDER_HINT = "X-Bellows-Provider-Id"
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    /**
     * Constant-time comparison to prevent timing attacks on the token.
     */
    private fun isAuthorized(session: IHTTPSession): Boolean {
        val stored = encryptedPrefs.getLocalApiToken() ?: return false
        val raw    = session.headers["authorization"] ?: return false
        val token  = if (raw.startsWith("Bearer ", ignoreCase = true))
            raw.substring(7).trim() else raw.trim()
        return token.length == stored.length && token.zip(stored).all { (a, b) -> a == b }
    }

    // ── Main router ────────────────────────────────────────────────────────────

    override fun serve(session: IHTTPSession): Response {
        // Health probe is the one auth-exempt endpoint
        if (session.method == Method.GET && session.uri == "/health") {
            return json(Response.Status.OK, """{"status":"ok","port":$PORT}""")
        }

        if (!isAuthorized(session)) {
            Log.w(TAG, "Unauthorized request from ${session.remoteIpAddress}")
            return json(
                Response.Status.UNAUTHORIZED,
                """{"error":{"message":"Invalid or missing Bearer token","type":"auth_error","code":401}}"""
            )
        }

        return try {
            when {
                session.method == Method.POST &&
                        session.uri.trimEnd('/').endsWith("/v1/chat/completions") ->
                    handleChat(session)

                session.method == Method.GET &&
                        session.uri.trimEnd('/').endsWith("/v1/models") ->
                    handleModels()

                session.method == Method.OPTIONS ->
                    cors(json(Response.Status.OK, "{}"))

                else ->
                    json(
                        Response.Status.NOT_FOUND,
                        """{"error":{"message":"Unknown endpoint: ${session.uri}","type":"not_found"}}"""
                    )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled error for ${session.method} ${session.uri}", e)
            json(
                Response.Status.INTERNAL_ERROR,
                """{"error":{"message":"${e.message?.replace("\"", "'")}","type":"server_error"}}"""
            )
        }
    }

    // ── POST /v1/chat/completions ──────────────────────────────────────────────

    private fun handleChat(session: IHTTPSession): Response {
        val body = mutableMapOf<String, String>()
        session.parseBody(body)
        val bodyStr = body["postData"]
            ?: return json(Response.Status.BAD_REQUEST,
                """{"error":{"message":"Empty request body"}}""")

        val req = runCatching { gson.fromJson(bodyStr, JsonObject::class.java) }.getOrNull()
            ?: return json(Response.Status.BAD_REQUEST,
                """{"error":{"message":"Invalid JSON body"}}""")

        val stream = req.get("stream")?.asBoolean ?: false

        // Provider selection: optional caller hint, otherwise best available
        val hintId = session.headers[HEADER_PROVIDER_HINT.lowercase()]
        val provider = runBlocking {
            if (hintId != null) providerConfigDao.getById(hintId) else null
                ?: rateLimitTracker.getRankedAvailableProviders().firstOrNull()
        } ?: return json(
            Response.Status.SERVICE_UNAVAILABLE,
            """{"error":{"message":"No LLM providers available","type":"no_provider","code":503}}"""
        )

        Log.d(TAG, "Routing to provider=${provider.id} stream=$stream")
        val messages = parseMessages(req)

        return if (stream) streamingResponse(provider, messages) else syncResponse(provider, messages)
    }

    // ── Streaming (SSE) ────────────────────────────────────────────────────────

    private fun streamingResponse(
        provider: com.anvil.bellows.data.local.db.entity.ProviderConfigEntity,
        messages: List<ChatMessage>
    ): Response {
        val pipe    = PipedOutputStream()
        val input   = PipedInputStream(pipe, 65_536)
        val id      = "chatcmpl-${UUID.randomUUID()}"
        val created = System.currentTimeMillis() / 1000L

        CoroutineScope(Dispatchers.IO).launch {
            try {
                llmRepository.streamChat(provider, messages)
                    .catch { e -> Log.e(TAG, "Stream error from ${provider.id}", e) }
                    .collect { token ->
                        val chunk = buildDeltaChunk(id, created, provider.selectedModel, token)
                        pipe.write("data: $chunk\n\n".toByteArray(Charsets.UTF_8))
                        pipe.flush()
                    }
                pipe.write("data: [DONE]\n\n".toByteArray(Charsets.UTF_8))
                pipe.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Streaming pipeline error", e)
            } finally {
                runCatching { pipe.close() }
            }
        }

        val response = newChunkedResponse(Response.Status.OK, "text/event-stream", input)
        response.addHeader("Cache-Control", "no-cache")
        response.addHeader("X-Accel-Buffering", "no")
        return cors(response)
    }

    // ── Non-streaming ──────────────────────────────────────────────────────────

    private fun syncResponse(
        provider: com.anvil.bellows.data.local.db.entity.ProviderConfigEntity,
        messages: List<ChatMessage>
    ): Response {
        val fullText = runBlocking {
            val sb = StringBuilder()
            llmRepository.streamChat(provider, messages)
                .catch { e -> Log.e(TAG, "Sync stream error from ${provider.id}", e) }
                .collect { sb.append(it) }
            sb.toString()
        }
        val id      = "chatcmpl-${UUID.randomUUID()}"
        val created = System.currentTimeMillis() / 1000L
        return cors(json(Response.Status.OK,
            buildCompletionResponse(id, created, provider.selectedModel, fullText)))
    }

    // ── GET /v1/models ─────────────────────────────────────────────────────────

    private fun handleModels(): Response {
        val providers = runBlocking { providerConfigDao.getEnabledProviders() }
        val arr = JsonArray()
        providers.forEach { p ->
            val obj = JsonObject().apply {
                addProperty("id", p.selectedModel)
                addProperty("object", "model")
                addProperty("created", 0)
                addProperty("owned_by", p.id)
            }
            arr.add(obj)
        }
        val result = JsonObject().apply {
            addProperty("object", "list")
            add("data", arr)
        }
        return cors(json(Response.Status.OK, gson.toJson(result)))
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun parseMessages(req: JsonObject): List<ChatMessage> {
        val arr = req.getAsJsonArray("messages") ?: return emptyList()
        return arr.mapIndexed { i, el ->
            val obj = el.asJsonObject
            ChatMessage(
                id      = "msg_$i",
                role    = obj.get("role")?.asString ?: "user",
                content = obj.get("content")?.asString ?: ""
            )
        }
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun buildDeltaChunk(id: String, created: Long, model: String, content: String): String =
        """{"id":"$id","object":"chat.completion.chunk","created":$created,"model":"${escapeJson(model)}","choices":[{"index":0,"delta":{"content":"${escapeJson(content)}"},"finish_reason":null}]}"""

    private fun buildCompletionResponse(id: String, created: Long, model: String, content: String): String {
        val tokens = estimateTokens(content)
        return """{"id":"$id","object":"chat.completion","created":$created,"model":"${escapeJson(model)}","choices":[{"index":0,"message":{"role":"assistant","content":"${escapeJson(content)}"},"finish_reason":"stop"}],"usage":{"prompt_tokens":0,"completion_tokens":$tokens,"total_tokens":$tokens}}"""
    }

    /**
     * Word-count–based token estimator consistent with [ConversationRepository].
     * Each whitespace-delimited word ≈ 1 token; each punctuation character ≈ 0.5 tokens.
     */
    private fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 1
        val words       = text.trim().split(Regex("\\s+")).size
        val punctuation = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        return (words + punctuation / 2).coerceAtLeast(1)
    }

    private fun json(status: Response.Status, body: String): Response =
        newFixedLengthResponse(status, "application/json", body)

    /** Add minimal CORS headers so web clients (e.g. OpenWebUI) can access the server. */
    private fun cors(response: Response): Response = response.apply {
        addHeader("Access-Control-Allow-Origin", "*")
        addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, $HEADER_PROVIDER_HINT")
    }
}
