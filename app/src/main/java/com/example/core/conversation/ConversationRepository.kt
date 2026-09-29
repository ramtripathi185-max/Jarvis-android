package com.example.core.conversation

import com.example.core.model.Message
import com.example.core.model.MessageSender
import com.example.data.db.JarvisDatabase
import com.example.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository interface for conversation messages.
 */
interface ConversationRepository {
    fun getMessages(): Flow<List<Message>>
    suspend fun addMessage(message: Message)
    suspend fun clearHistory()
    suspend fun seedWelcomeIfNeeded()
}

/**
 * Room-backed implementation of ConversationRepository.
 */
class RoomConversationRepository(
    private val database: JarvisDatabase
) : ConversationRepository {

    private val dao = database.messageDao()

    override fun getMessages(): Flow<List<Message>> {
        return dao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMessage(message: Message) {
        dao.insertMessage(MessageEntity.fromDomain(message))
    }

    override suspend fun clearHistory() {
        dao.clearAllMessages()
    }

    override suspend fun seedWelcomeIfNeeded() {
        if (dao.getMessageCount() == 0) {
            val welcome = Message(
                id = UUID.randomUUID().toString(),
                text = "JARVIS operational. All core systems calibrated. How may I assist you today, sir?",
                sender = MessageSender.JARVIS,
                timestamp = System.currentTimeMillis()
            )
            addMessage(welcome)
        }
    }
}
