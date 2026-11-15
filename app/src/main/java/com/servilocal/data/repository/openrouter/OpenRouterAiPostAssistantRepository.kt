package com.servilocal.data.repository.openrouter

import android.util.Log
import com.google.gson.JsonParser
import com.servilocal.BuildConfig
import com.servilocal.data.remote.openrouter.*
import com.servilocal.domain.model.*
import com.servilocal.domain.repository.AiPostAssistantRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import retrofit2.HttpException

@Singleton
class OpenRouterAiPostAssistantRepository @Inject constructor(
    private val api: OpenRouterApi,
    private val promptBuilder: PostAssistantPromptBuilder
) : AiPostAssistantRepository {
    override suspend fun ask(request: AiPostAssistantRequest): AiPostAssistantReply {
        if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Request skipped: OPENROUTER_API_KEY is empty")
            return AiPostAssistantReply.Failure
        }
        val models = (listOf(BuildConfig.OPENROUTER_MODEL) + BuildConfig.OPENROUTER_FALLBACK_MODELS.split(",") + FREE_MODEL)
            .map(String::trim).filter { it.endsWith(":free") }.distinct()
        for (model in models) {
            try {
                // OpenRouter requires `model`; send one candidate per request so each fallback
                // remains compatible with providers that reject the optional `models` array.
                val response = api.createChatCompletion(
                    authorization = "Bearer ${BuildConfig.OPENROUTER_API_KEY}",
                    request = ChatCompletionRequest(model = model, messages = promptBuilder.buildMessages(request))
                )
                val content = response.choices.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) return promptBuilder.parseResponse(content)
                if (BuildConfig.DEBUG) Log.w(TAG, "Empty response from configured model")
            } catch (error: HttpException) {
                if (BuildConfig.DEBUG) {
                    val requestId = error.response()?.headers()?.get("x-request-id")
                    val detail = error.response()?.errorBody()?.string()?.let(::safeApiErrorMessage)
                    Log.w(TAG, "HTTP ${error.code()}${requestId?.let { ", requestId=$it" }.orEmpty()}${detail?.let { ": $it" }.orEmpty()}")
                }
            } catch (error: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Request failed: ${error.javaClass.simpleName}")
            }
        }
        return AiPostAssistantReply.Failure
    }

    private fun safeApiErrorMessage(body: String): String? = try {
        JsonParser.parseString(body).asJsonObject.getAsJsonObject("error")?.get("message")
            ?.asString?.replace(Regex("[\\r\\n]+"), " ")?.take(300)
    } catch (_: Exception) { null }

    companion object {
        private const val TAG = "OpenRouterAssistant"
        private const val FREE_MODEL = "meta-llama/llama-3.3-70b-instruct:free"
    }
}
