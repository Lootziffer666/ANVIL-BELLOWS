package com.anvil.bellows.server

import android.util.Log
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LocalProxyGateway"

/**
 * Policy-Schicht-Brücke zum lokalen LiteLLM-Proxy (Termux, [DEFAULT_BASE]).
 *
 * Architektur: ANVIL-BELLOWS ist die POLICY-Schicht (Auth, Free-Tier-Wahl,
 * Self-Learning-Limits, Key-Vault); LiteLLM ist die ROUTING-Engine (140+
 * Provider). Wenn der lokale Proxy läuft, reicht [BellowsHttpServer] OpenAI-
 * Anfragen hierdurch — sonst fällt er auf den eingebauten nativen Router zurück
 * (Kaltstart-Sicherung, KEIN „Light-Modus").
 *
 * Auth: Der LiteLLM-`master_key` wird gleich dem lokalen Bellows-Token gesetzt
 * (siehe setup_termux.sh) → keine zusätzliche Schlüssel-Speicherung nötig.
 *
 * Verfügbarkeit wird kurz gecacht ([AVAIL_TTL_MS]), damit ein nicht laufender
 * Proxy nicht jede Anfrage um den Probe-Timeout verzögert.
 */
@Singleton
class LocalProxyGateway @Inject constructor(
    private val encryptedPrefs: EncryptedPrefsManager
) {
    companion object {
        const val DEFAULT_BASE = "http://127.0.0.1:4000"
        private const val AVAIL_TTL_MS = 10_000L
    }

    // Probe: sehr kurze Timeouts → schneller Fallback, wenn kein Proxy läuft.
    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    // Forward: kein Read-Timeout (Streaming/SSE kann lange offen bleiben).
    private val forwardClient = OkHttpClient.Builder()
        .connectTimeout(800, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile private var lastCheckMs = 0L
    @Volatile private var lastAvailable = false

    private fun baseUrl(): String = DEFAULT_BASE.trimEnd('/')

    /** True, wenn der lokale LiteLLM-Proxy auf /health/readiness antwortet (gecacht). */
    fun isAvailable(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastCheckMs < AVAIL_TTL_MS) return lastAvailable
        lastAvailable = try {
            val req = Request.Builder().url("${baseUrl()}/health/readiness").get().build()
            probeClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
        lastCheckMs = now
        return lastAvailable
    }

    /**
     * Reicht eine OpenAI-kompatible Anfrage an den lokalen Proxy durch.
     * @param path z. B. "/v1/chat/completions" oder "/v1/models"
     * @param jsonBody POST-Body (null → GET)
     * @return NanoHTTPD-Response (Streaming wird 1:1 durchgereicht) oder null bei
     *         Verbindungsfehler → der Aufrufer fällt auf den nativen Router zurück.
     */
    fun forward(path: String, jsonBody: String?): NanoHTTPD.Response? {
        val token = encryptedPrefs.getLocalApiToken()
        return try {
            val builder = Request.Builder().url("${baseUrl()}$path")
            if (jsonBody != null) {
                builder.post(jsonBody.toRequestBody("application/json".toMediaType()))
            } else {
                builder.get()
            }
            if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")

            val resp = forwardClient.newCall(builder.build()).execute()
            val status = Status.lookup(resp.code) ?: Status.OK
            val contentType = resp.header("Content-Type") ?: "application/json"

            if (contentType.contains("text/event-stream", ignoreCase = true)) {
                // SSE 1:1 durchreichen — Body-Stream direkt als chunked Response.
                val out = NanoHTTPD.newChunkedResponse(status, contentType, resp.body?.byteStream())
                out.addHeader("Cache-Control", "no-cache")
                out.addHeader("X-Accel-Buffering", "no")
                out
            } else {
                NanoHTTPD.newFixedLengthResponse(status, contentType, resp.body?.string() ?: "")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Local proxy forward failed (${e.message}) → Fallback auf nativen Router")
            null
        }
    }
}
