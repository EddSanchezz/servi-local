package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.*
import com.servilocal.domain.usecase.posts.AskPostAssistantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AiChatRole { USER, ASSISTANT }
data class AiChatMessageUi(val id: String = UUID.randomUUID().toString(), val role: AiChatRole, val text: String, val suggestion: AiPostSuggestion? = null, val isError: Boolean = false)
data class AiPostAssistantUiState(val messages: List<AiChatMessageUi> = emptyList(), val input: String = "", val isSending: Boolean = false, val canRetry: Boolean = false)

@HiltViewModel
class AiPostAssistantViewModel @Inject constructor(private val ask: AskPostAssistantUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(AiPostAssistantUiState())
    val uiState = _uiState.asStateFlow()
    private var lastUserText = ""

    fun onInputChange(value: String) { _uiState.update { it.copy(input = value) } }
    fun reset(welcome: String) { lastUserText = ""; _uiState.value = AiPostAssistantUiState(messages = listOf(AiChatMessageUi(role = AiChatRole.ASSISTANT, text = welcome))) }
    fun sendMessage(mode: AiPostAssistantMode, currentTitle: String, currentDescription: String) {
        val text = _uiState.value.input.trim()
        if (text.isBlank() || _uiState.value.isSending) return
        lastUserText = text
        _uiState.update { it.copy(input = "", isSending = true, canRetry = false, messages = it.messages + AiChatMessageUi(role = AiChatRole.USER, text = text)) }
        askReply(mode, currentTitle, currentDescription)
    }
    fun retryLast(currentTitle: String, currentDescription: String) {
        if (lastUserText.isBlank() || _uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true, canRetry = false) }
        askReply(if (currentTitle.isBlank() && currentDescription.isBlank()) AiPostAssistantMode.CREATE else AiPostAssistantMode.IMPROVE, currentTitle, currentDescription)
    }
    private fun askReply(mode: AiPostAssistantMode, title: String, description: String) = viewModelScope.launch {
        val history = _uiState.value.messages.map {
            AiPostAssistantMessage(if (it.role == AiChatRole.USER) AiPostAssistantMessageRole.USER else AiPostAssistantMessageRole.ASSISTANT, it.text)
        }.takeLast(10)
        val reply = ask(AiPostAssistantRequest(mode, history, title, description))
        val message = when (reply) {
            is AiPostAssistantReply.Question -> AiChatMessageUi(role = AiChatRole.ASSISTANT, text = reply.message)
            is AiPostAssistantReply.Suggestion -> AiChatMessageUi(role = AiChatRole.ASSISTANT, text = reply.message, suggestion = reply.suggestion)
            AiPostAssistantReply.OutOfScope -> AiChatMessageUi(role = AiChatRole.ASSISTANT, text = "Está fuera de mis capacidades.")
            AiPostAssistantReply.Failure -> AiChatMessageUi(role = AiChatRole.ASSISTANT, text = "No entendí tu respuesta. Intenta de nuevo.", isError = true)
        }
        _uiState.update { it.copy(isSending = false, canRetry = message.isError, messages = it.messages + message) }
    }
}
