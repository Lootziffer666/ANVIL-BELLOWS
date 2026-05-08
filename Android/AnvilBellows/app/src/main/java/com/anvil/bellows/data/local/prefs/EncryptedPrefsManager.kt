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

    fun storeApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("api_key_$providerId", apiKey).apply()
    }

    fun getApiKey(providerId: String): String? =
        prefs.getString("api_key_$providerId", null)

    fun removeApiKey(providerId: String) {
        prefs.edit().remove("api_key_$providerId").apply()
    }

    fun storeVertexServiceAccountJson(json: String) {
        prefs.edit().putString("vertex_sa_json", json).apply()
    }

    fun getVertexServiceAccountJson(): String? =
        prefs.getString("vertex_sa_json", null)

    fun storeWikiEndpoint(url: String) {
        prefs.edit().putString("wiki_endpoint", url).apply()
    }

    fun getWikiEndpoint(): String? =
        prefs.getString("wiki_endpoint", null)

    fun storeWikiToken(token: String) {
        prefs.edit().putString("wiki_token", token).apply()
    }

    fun getWikiToken(): String? =
        prefs.getString("wiki_token", null)

    fun isFirstLaunch(): Boolean = prefs.getBoolean("first_launch", true)

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean("first_launch", false).apply()
    }

    fun storeString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String? = null): String? =
        prefs.getString(key, default)

    fun getOrCreateLocalApiToken(): String {
        return getString("local_api_token") ?: java.util.UUID.randomUUID().toString().also {
            storeString("local_api_token", it)
        }
    }
}
