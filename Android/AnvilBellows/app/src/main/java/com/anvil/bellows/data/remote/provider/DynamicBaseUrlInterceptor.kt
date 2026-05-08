package com.anvil.bellows.data.remote.provider

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val targetBase = original.header("X-Target-Base-Url")
            ?: return chain.proceed(original)

        val parsedBase = targetBase.trimEnd('/').toHttpUrl()

        // Combine base path with the endpoint path from the Retrofit @POST annotation.
        // e.g. base="/openai/" + endpoint="v1/chat/completions" → "/openai/v1/chat/completions"
        val basePath = parsedBase.encodedPath.trimEnd('/')
        val originalPath = original.url.encodedPath.trimStart('/')
        val combinedPath = if (basePath.isEmpty()) "/$originalPath" else "$basePath/$originalPath"

        val newUrl = original.url.newBuilder()
            .scheme(parsedBase.scheme)
            .host(parsedBase.host)
            .port(parsedBase.port)
            .encodedPath(combinedPath)
            .build()

        val newRequest = original.newBuilder()
            .url(newUrl)
            .removeHeader("X-Target-Base-Url")
            .build()

        return chain.proceed(newRequest)
    }
}
