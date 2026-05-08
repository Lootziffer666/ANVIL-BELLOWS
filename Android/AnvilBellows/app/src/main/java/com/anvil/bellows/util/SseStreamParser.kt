package com.anvil.bellows.util

import com.anvil.bellows.data.remote.dto.StreamChunk
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody

object SseStreamParser {

    fun parseStream(body: ResponseBody, gson: Gson): Flow<String> = flow {
        body.charStream().buffered().use { br ->
            var line = br.readLine()
            while (line != null) {
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data != "[DONE]" && data.isNotBlank()) {
                        try {
                            val chunk = gson.fromJson(data, StreamChunk::class.java)
                            val token = chunk?.choices?.firstOrNull()?.delta?.content
                            if (!token.isNullOrEmpty()) emit(token)
                        } catch (_: JsonSyntaxException) {
                            // Skip malformed lines (common with some providers)
                        }
                    }
                }
                line = br.readLine()
            }
        }
    }.flowOn(Dispatchers.IO)
}
