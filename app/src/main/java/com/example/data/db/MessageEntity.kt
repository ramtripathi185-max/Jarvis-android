package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.Message
import com.example.core.model.MessageSender

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val sender: String,
    val timestamp: Long
) {
    fun toDomain(): Message {
        return Message(
            id = id,
            text = text,
            sender = try { MessageSender.valueOf(sender) } catch (e: Exception) { MessageSender.JARVIS },
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(domain: Message): MessageEntity {
            return MessageEntity(
                id = domain.id,
                text = domain.text,
                sender = domain.sender.name,
                timestamp = domain.timestamp
            )
        }
    }
}
