package com.example.core.conversation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationRepository {
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val allMessages: Flow<List<String>> = _messages.asStateFlow()

    suspend fun addMessage(message: String) {
        _messages.value = _messages.value + message
    }

    suspend fun clearAllMessages() {
        _messages.value = emptyList()
    }
}
