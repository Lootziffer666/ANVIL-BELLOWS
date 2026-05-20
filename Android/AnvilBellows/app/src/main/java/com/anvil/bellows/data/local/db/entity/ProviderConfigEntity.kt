package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted provider configuration.
 *
 * SCHEMA VERSION HISTORY
 * ─────────────────────────────────────────────────────────────────────────────
 * v1 → v2 : added registrationUrl
 * v2 → v3 : added agent_presets table + conversation_sessions.presetId
 * v3 → v4 : added consoleUrl, authHeaderName, noAuth, deprecated,
 *            deprecationNotice, serviceKinds, clipboardPattern
 *
 * INVARIANT: Never rename or drop a column without a corresponding Room
 * migration. Add new columns with NOT NULL + DEFAULT in the migration SQL.
 */
@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val apiKeyAlias: String,
    val rpmLimit: Int,
    val rpdLimit: Int,
    val contextWindow: Int,
    val maxOutput: Int,
    val tier: Int,
    val isByok: Boolean,
    val enabled: Boolean,
    val isCustom: Boolean = false,
    val authType: String = "API_KEY",
    val vertexProjectId: String? = null,
    val vertexLocation: String? = null,
    val selectedModel: String,
    val notes: String = "",
    val registrationUrl: String = "",
    // ── v4 columns ────────────────────────────────────────────────────────────
    /** Direct link to the API key console. Populated from ProviderDefault.consoleUrl. */
    val consoleUrl: String = "",
    /** Header name for the auth credential ("Authorization", "x-api-key", …). */
    val authHeaderName: String = "Authorization",
    /** True for providers that need no key (e.g. Pollinations). */
    val noAuth: Boolean = false,
    val deprecated: Boolean = false,
    val deprecationNotice: String = "",
    /** Comma-separated service kinds, e.g. "llm" or "llm,tts". */
    val serviceKinds: String = "llm",
    /** Regex pattern to detect this provider's key on the clipboard. */
    val clipboardPattern: String = ""
)
