package com.anvil.bellows.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageDto(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null,
    @SerializedName("top_p") val topP: Double? = null
)

data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?,
    val model: String? = null
)

data class Choice(
    val message: MessageDto,
    @SerializedName("finish_reason") val finishReason: String?,
    val index: Int = 0
)

data class StreamChunk(
    val id: String? = null,
    val choices: List<StreamChoice> = emptyList()
)

data class StreamChoice(
    val delta: DeltaContent,
    @SerializedName("finish_reason") val finishReason: String?,
    val index: Int = 0
)

data class DeltaContent(
    val role: String? = null,
    val content: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0
)

data class ModelsResponse(
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    @SerializedName("owned_by") val ownedBy: String? = null
)
