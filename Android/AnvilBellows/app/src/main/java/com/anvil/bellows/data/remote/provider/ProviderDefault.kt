package com.anvil.bellows.data.remote.provider

// ── Specialties ──────────────────────────────────────────────────────────────
enum class Specialty { GENERAL, CODING, REASONING, VISION, FAST }

// ── Model-level defaults ──────────────────────────────────────────────────────
data class ModelDefault(
    val modelId: String,
    val displayName: String = modelId,
    val contextWindow: Int,
    val maxOutput: Int,
    val rpmLimit: Int = Int.MAX_VALUE,
    val rpdLimit: Int = Int.MAX_VALUE,
    val supportsVision: Boolean = false,
    val supportsReasoning: Boolean = false,
    val specialties: Set<Specialty> = setOf(Specialty.GENERAL)
)

// ── Provider-level defaults ───────────────────────────────────────────────────
//
// INVARIANT: id must be unique and stable (used as Room PK + EncryptedPrefs key).
// Do NOT change an existing id – create a new entry and deprecate the old one instead.
//
// authType values:
//   "API_KEY"   – standard Bearer token (Authorization: Bearer <key>)
//   "VERTEX"    – Google Service Account JSON (stored separately in EncryptedPrefs)
//   "NO_AUTH"   – no authentication required
//
// authHeaderName: the HTTP header that carries the token.  Most providers use
//   "Authorization" with a "Bearer <token>" value; Anthropic uses "x-api-key"
//   with the raw token.  This field is informational metadata used by the wizard
//   and future interceptor improvements; the actual request auth is handled by
//   LlmHeaderInterceptor / VertexAuthInterceptor.
//
// clipboardPattern: Java/Kotlin regex that matches a valid API key for this
//   provider.  Used by ApiKeyWizardScreen to auto-detect a copied key.
//
data class ProviderDefault(
    val id: String,
    val name: String,
    val baseUrl: String,
    val models: List<ModelDefault>,
    val rpmLimit: Int,
    val rpdLimit: Int,
    val contextWindow: Int,
    val maxOutput: Int,
    val tier: Int,
    val isByok: Boolean = false,
    val authType: String = "API_KEY",
    val notes: String = "",
    /** Legacy alias kept for DB compat – prefer consoleUrl. */
    val registrationUrl: String = "",
    // ── Fields added in v4 ────────────────────────────────────────────────────
    /** Direct URL to the API key / credentials console (opened by the Wizard). */
    val consoleUrl: String = registrationUrl,
    /** HTTP header name for the credential (e.g. "Authorization", "x-api-key"). */
    val authHeaderName: String = "Authorization",
    /** True for providers that require no API key at all. */
    val noAuth: Boolean = false,
    val deprecated: Boolean = false,
    val deprecationNotice: String = "",
    /** Service categories this provider supports (e.g. "llm", "tts", "image"). */
    val serviceKinds: List<String> = listOf("llm"),
    /** Regex matching a freshly copied key.  Empty string = no auto-detection. */
    val clipboardPattern: String = ""
)
