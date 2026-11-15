package com.servilocal.data.remote.openrouter

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://servilocal.app",
        @Header("X-Title") title: String = "ServiLocal",
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val models: List<String>? = null,
    val model: String? = null,
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.4,
    @SerializedName("max_tokens") val maxTokens: Int = 700,
    @SerializedName("top_p") val topP: Double = 0.9
)
data class ChatMessageDto(val role: String, val content: String)
data class ChatCompletionResponse(val choices: List<ChoiceDto> = emptyList())
data class ChoiceDto(val message: ChatMessageDto, @SerializedName("finish_reason") val finishReason: String? = null)
