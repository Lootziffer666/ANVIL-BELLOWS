package com.anvil.bellows.util

import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.google.auth.oauth2.ServiceAccountCredentials
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VertexAuthInterceptor @Inject constructor(
    private val encryptedPrefs: EncryptedPrefsManager
) : Interceptor {

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiry: Long = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val targetBase = request.header("X-Target-Base-Url") ?: ""

        if (!targetBase.contains("aiplatform.googleapis.com")) {
            return chain.proceed(request)
        }

        val token = getValidToken() ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }

    @Synchronized
    private fun getValidToken(): String? {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiry - 60_000L) return cachedToken

        val saJson = encryptedPrefs.getVertexServiceAccountJson() ?: return null
        return try {
            val credentials = ServiceAccountCredentials
                .fromStream(saJson.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

            val token = credentials.refreshAccessToken()
            cachedToken = token.tokenValue
            tokenExpiry = token.expirationTime?.time ?: (now + 3_600_000L)
            cachedToken
        } catch (e: Exception) {
            null
        }
    }

    fun invalidateToken() {
        cachedToken = null
        tokenExpiry = 0L
    }
}
