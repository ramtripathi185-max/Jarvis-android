package com.example.core.engine

import com.example.core.conversation.ConversationRepository

class DefaultAssistantEngine(
    private val repository: ConversationRepository
) {
    suspend fun processUserQuery(query: String): String {
        repository.addMessage(query)
        return "Processed: $query"
    }
}
