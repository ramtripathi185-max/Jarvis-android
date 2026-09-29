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
    val timestamp: Long,
    val isVoice: Boolean
) {
    fun toDomain(): Message {
        val domainSender = try {
            MessageSender.valueOf(sender)
        } catch (e: Exception) {
            MessageSender.SYSTEM
        }
        return Message(
            id = id,
            text = text,
            sender = domainSender,
            timestamp = timestamp,
            isVoice = isVoice
        )
    }

    companion object {
        fun fromDomain(message: Message): MessageEntity {
            return MessageEntity(
                id = message.id,
                text = message.text,
                sender = message.sender.name,
                timestamp = message.timestamp,
                isVoice = message.isVoice
            )
        }
    }
}
