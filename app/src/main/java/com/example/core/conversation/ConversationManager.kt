package com.example.core.conversation

class ConversationManager(
    private val repository: ConversationRepository
) {
    suspend fun processMessage(msg: String) {
        repository.addMessage(msg)
    }
}
