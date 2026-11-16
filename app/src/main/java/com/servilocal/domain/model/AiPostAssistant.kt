package com.servilocal.domain.model

enum class AiPostAssistantMode { CREATE, IMPROVE }
enum class AiPostAssistantMessageRole { USER, ASSISTANT }

data class AiPostSuggestion(val title: String, val description: String)
data class AiPostAssistantMessage(val role: AiPostAssistantMessageRole, val content: String)
data class AiPostAssistantRequest(
    val mode: AiPostAssistantMode,
    val chatHistory: List<AiPostAssistantMessage>,
    val currentTitle: String,
    val currentDescription: String
)

sealed interface AiPostAssistantReply {
    data class Question(val message: String) : AiPostAssistantReply
    data class Suggestion(val message: String, val suggestion: AiPostSuggestion) : AiPostAssistantReply
    data object OutOfScope : AiPostAssistantReply
    data object Failure : AiPostAssistantReply
}
