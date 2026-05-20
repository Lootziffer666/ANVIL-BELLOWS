package com.anvil.bellows.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPrefsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val KEY_LOCAL_API_TOKEN = "local_api_token"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "anvil_bellows_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Provider API keys ──────────────────────────────────────────────────────

    fun storeApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("api_key_$providerId", apiKey).apply()
    }

    fun getApiKey(providerId: String): String? =
        prefs.getString("api_key_$providerId", null)

    fun removeApiKey(providerId: String) {
        prefs.edit().remove("api_key_$providerId").apply()
    }

    // ── Vertex AI Service Account JSON ─────────────────────────────────────────

    fun storeVertexServiceAccountJson(json: String) {
        prefs.edit().putString("vertex_sa_json", json).apply()
    }

    fun getVertexServiceAccountJson(): String? =
        prefs.getString("vertex_sa_json", null)

    // ── App lifecycle ──────────────────────────────────────────────────────────

    fun isFirstLaunch(): Boolean = prefs.getBoolean("first_launch", true)

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean("first_launch", false).apply()
    }

    // ── Generic helpers ────────────────────────────────────────────────────────

    fun storeString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String? = null): String? =
        prefs.getString(key, default)

    // ── Local NanoHTTPD server token ───────────────────────────────────────────

    /**
     * Returns the stored local API token, or null if none has been set yet.
     * Used by [LocalServerViewModel] to populate the UI without auto-generating.
     */
    fun getLocalApiToken(): String? =
        prefs.getString(KEY_LOCAL_API_TOKEN, null)

    /**
     * Stores a caller-supplied local API token.
     * Used by [LocalServerViewModel.generateToken] / [LocalServerViewModel.regenerateToken].
     */
    fun storeLocalApiToken(token: String) {
        prefs.edit().putString(KEY_LOCAL_API_TOKEN, token).apply()
    }

    /**
     * Returns the existing token or auto-generates and persists a new UUID-based one.
     * Kept for backward compatibility with existing NanoHTTPD server setup code.
     */
    fun getOrCreateLocalApiToken(): String =
        getLocalApiToken() ?: java.util.UUID.randomUUID().toString().also {
            storeLocalApiToken(it)
        }
}
