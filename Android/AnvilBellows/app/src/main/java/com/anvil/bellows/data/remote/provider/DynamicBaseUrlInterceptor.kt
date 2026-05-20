package com.anvil.bellows.data.remote.provider

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Replaces the Retrofit base URL at call-time based on the
 * [X_TARGET_BASE_URL] request header.
 *
 * This allows a single Retrofit service instance to route to different
 * provider endpoints without rebuilding the service.
 *
 * ── Path combination logic ──────────────────────────────────────────────────
 *
 * Given:
 *   targetBase    = "https://api.anthropic.com/v1/"
 *   Retrofit path = "v1/chat/completions"           (from @POST annotation)
 *
 * Naïve concatenation produces "/v1/v1/chat/completions" — a duplicate-segment
 * bug triggered whenever the base URL already includes the version prefix that
 * Retrofit appends to the relative endpoint path.
 *
 * Fix: if the last path segment of basePath equals the first path segment of
 * the Retrofit endpoint path, the duplicate is stripped:
 *
 *   basePath         = "/v1"     lastSegment = "v1"
 *   origPath (raw)   = "v1/chat/completions"
 *   firstSegment     = "v1"   → match → strip → "chat/completions"
 *   combinedPath     = "/v1/chat/completions"  ✓
 *
 * For a base URL that does NOT end with the version prefix
 * (e.g. "https://api.groq.com/openai/") the logic is a no-op:
 *
 *   basePath         = "/openai"  lastSegment = "openai"
 *   origPath (raw)   = "v1/chat/completions"
 *   firstSegment     = "v1"       → no match → keep as-is
 *   combinedPath     = "/openai/v1/chat/completions"  ✓
 *
 * ── noAuth handling ────────────────────────────────────────────────────────
 *
 * Providers with noAuth=true (e.g. Pollinations) must not receive any
 * Authorization header. Callers pass an empty string as the authorization
 * parameter; this interceptor detects blank/empty Authorization headers and
 * removes them before forwarding the request.
 *
 * INVARIANT: this interceptor is stateless and thread-safe.
 *
 * @see com.anvil.bellows.data.remote.api.LlmApiService
 */
class DynamicBaseUrlInterceptor : Interceptor {

    companion object {
        const val X_TARGET_BASE_URL = "X-Target-Base-Url"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val targetBase = original.header(X_TARGET_BASE_URL)
            ?: return chain.proceed(original)

        val parsedBase = targetBase.trimEnd('/').toHttpUrl()
        val basePath   = parsedBase.encodedPath.trimEnd('/')       // e.g. "/v1" or "/openai" or ""
        val origPath   = original.url.encodedPath.trimStart('/')   // e.g. "v1/chat/completions"

        // ── De-duplicate prefix segment ─────────────────────────────────────
        // Only strip when the basePath's last segment == origPath's first segment
        // to avoid a spurious double-version-prefix in the final URL.
        val deduped = if (basePath.isNotEmpty()) {
            val lastBase  = basePath.substringAfterLast('/')
            val firstOrig = origPath.substringBefore('/')
            if (lastBase.isNotEmpty() && lastBase == firstOrig) {
                // e.g. basePath="/v1", origPath="v1/chat/completions" → "chat/completions"
                origPath.removePrefix("$firstOrig/")
            } else {
                origPath
            }
        } else {
            origPath
        }

        val combinedPath = if (basePath.isEmpty()) "/$deduped" else "$basePath/$deduped"

        val newUrl = parsedBase.newBuilder()
            .encodedPath(combinedPath)
            .apply {
                // Carry over any query parameters from the original request
                original.url.queryParameterNames.forEach { name ->
                    original.url.queryParameterValues(name).forEach { value ->
                        addQueryParameter(name, value)
                    }
                }
            }
            .build()

        var newRequest = original.newBuilder()
            .url(newUrl)
            .removeHeader(X_TARGET_BASE_URL)
            .build()

        // ── noAuth: strip empty Authorization header ─────────────────────────
        // Providers with noAuth=true have callers pass "" as the auth value.
        // An empty/blank Authorization header must not be forwarded — many
        // endpoints treat any Authorization header (even empty) as an auth
        // attempt and reject the request before the body is read.
        if (newRequest.header("Authorization").isNullOrBlank()) {
            newRequest = newRequest.newBuilder()
                .removeHeader("Authorization")
                .build()
        }

        return chain.proceed(newRequest)
    }
}
