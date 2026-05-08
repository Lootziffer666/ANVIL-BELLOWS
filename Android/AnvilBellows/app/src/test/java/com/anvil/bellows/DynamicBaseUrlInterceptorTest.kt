package com.anvil.bellows

import com.anvil.bellows.data.remote.provider.DynamicBaseUrlInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DynamicBaseUrlInterceptorTest {

    private val server = MockWebServer()
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `base path is preserved when combining with endpoint path`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val baseUrl = "http://${server.hostName}:${server.port}/openai/"

        val request = Request.Builder()
            .url("http://placeholder/v1/chat/completions")
            .header("X-Target-Base-Url", baseUrl)
            .get()
            .build()

        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("/openai/v1/chat/completions", recorded.path)
    }

    @Test
    fun `empty base path produces correct combined path`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val baseUrl = "http://${server.hostName}:${server.port}/"

        val request = Request.Builder()
            .url("http://placeholder/v1/chat/completions")
            .header("X-Target-Base-Url", baseUrl)
            .get()
            .build()

        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
    }

    @Test
    fun `no header leaves request unchanged`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val originalUrl = "http://${server.hostName}:${server.port}/some/path"
        val request = Request.Builder()
            .url(originalUrl)
            .get()
            .build()

        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("/some/path", recorded.path)
    }
}
