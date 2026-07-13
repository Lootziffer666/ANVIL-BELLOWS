package com.anvil.bellows

import com.anvil.bellows.data.remote.dto.ChatRequest
import com.anvil.bellows.data.remote.dto.ImageUrlDto
import com.anvil.bellows.data.remote.dto.MessageContentPartDto
import com.anvil.bellows.data.remote.dto.MessageDto
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionMessageContractTest {

    private val gson = Gson()

    @Test
    fun `text-only message content remains a string`() {
        val request = ChatRequest(
            model = "text-model",
            messages = listOf(MessageDto(role = "user", content = "Hello")),
            stream = true
        )

        val json = gson.toJsonTree(request).asJsonObject
        val content = json.getAsJsonArray("messages")[0].asJsonObject.get("content")

        assertTrue(content.isJsonPrimitive)
        assertEquals("Hello", content.asString)
    }

    @Test
    fun `vision message content serializes as OpenAI-compatible content parts`() {
        val parts = listOf(
            MessageContentPartDto(type = "text", text = "Describe this image"),
            MessageContentPartDto(
                type = "image_url",
                imageUrl = ImageUrlDto(url = "data:image/png;base64,abc123", detail = "high")
            )
        )
        val request = ChatRequest(
            model = "vision-model",
            messages = listOf(MessageDto(role = "user", content = parts)),
            stream = true
        )

        val json = gson.toJsonTree(request).asJsonObject
        val content = json.getAsJsonArray("messages")[0].asJsonObject.get("content")

        assertTrue(content is JsonArray)
        val contentParts = content.asJsonArray
        assertEquals("text", contentParts[0].asJsonObject.get("type").asString)
        assertEquals("Describe this image", contentParts[0].asJsonObject.get("text").asString)
        assertEquals("image_url", contentParts[1].asJsonObject.get("type").asString)
        assertEquals("data:image/png;base64,abc123", contentParts[1].asJsonObject.getAsJsonObject("image_url").get("url").asString)
        assertEquals("high", contentParts[1].asJsonObject.getAsJsonObject("image_url").get("detail").asString)
    }
}
