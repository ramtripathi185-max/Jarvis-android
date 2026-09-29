package com.example.core.conversation

import com.example.core.model.LanguageOption
import com.example.core.model.Message
import com.example.core.model.MessageSender
import java.util.UUID

/**
 * Manages active dialogue formatting, personality prompt generation, and history tracking.
 */
class ConversationManager(
    private val repository: ConversationRepository
) {
    /**
     * Builds the dedicated JARVIS system instruction customized for personality and selected language.
     */
    fun buildSystemInstruction(language: LanguageOption, customToneNote: String = ""): String {
        val basePrompt = """
            You are JARVIS, an advanced, highly intelligent, respectful, calm, and concise AI personal assistant.
            Personality attributes:
            - Calm, collected, and unfazed under any condition.
            - Highly capable, thoughtful, and articulate.
            - Polite, respectful ("sir", "certainly", "right away"), yet warm and natural.
            - Keep your responses direct, concise, and ideally under 2-3 sentences so they sound natural when spoken aloud over text-to-speech.
            - Never simulate or impersonate movie dialogue verbatim or copyrighted character scripts; maintain your own unique sophisticated persona.
        """.trimIndent()

        val languageGuide = when (language) {
            LanguageOption.HINDI -> """
                Language preference: Respond in clear, natural, respectful Hindi (Devanagari or Romanized if appropriate for speech), polite and elegant.
            """.trimIndent()
            LanguageOption.HINGLISH -> """
                Language preference: Respond in natural, conversational Hinglish (blend of Hindi and English written in Latin script), just like modern bilingual speakers converse effortlessly. Keep it polite, smart, and crisp.
            """.trimIndent()
            LanguageOption.ENGLISH_INDIA, LanguageOption.ENGLISH -> """
                Language preference: Respond in clean, articulate English.
            """.trimIndent()
        }

        return if (customToneNote.isBlank()) {
            "$basePrompt\n$languageGuide"
        } else {
            "$basePrompt\n$languageGuide\nAdditional user preference: $customToneNote"
        }
    }

    suspend fun recordUserMessage(text: String, isVoice: Boolean = false): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            sender = MessageSender.USER,
            timestamp = System.currentTimeMillis(),
            isVoice = isVoice
        )
        repository.addMessage(message)
        return message
    }

    suspend fun recordAssistantMessage(text: String): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            sender = MessageSender.JARVIS,
            timestamp = System.currentTimeMillis()
        )
        repository.addMessage(message)
        return message
    }

    suspend fun recordSystemMessage(text: String): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            sender = MessageSender.SYSTEM,
            timestamp = System.currentTimeMillis()
        )
        repository.addMessage(message)
        return message
    }
}
