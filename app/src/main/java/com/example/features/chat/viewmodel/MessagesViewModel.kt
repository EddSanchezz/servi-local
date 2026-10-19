package com.example.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MessagesUiState(val segment: String = "client")

class MessagesViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()
    val conversations = repository.conversations
    fun setSegment(value: String) = _uiState.update { it.copy(segment = value) }
    fun markConversationRead(id: String) = repository.markConversationRead(id)
}
