package com.example.core.conversation

import com.example.core.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: Flow<List<Message>> = _messages.asStateFlow()

    fun getMessages(): Flow<List<Message>> {
        return messages
    }

    suspend fun saveMessage(message: Message) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList
    }

    suspend fun clearAllMessages() {
        _messages.value = emptyList()
    }
}

