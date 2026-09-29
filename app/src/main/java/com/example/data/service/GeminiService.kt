package com.example.data.service

import com.example.core.model.Message

/**
 * Dedicated service interface for Google Gemini API.
 * Encapsulates network communication, token limits, and model parameters.
 * Modular structure allows introducing Gemini Live real-time audio in Part 2.
 */
interface GeminiService {
    val isApiKeyConfigured: Boolean

    suspend fun generateResponse(
        userMessage: String,
        conversationHistory: List<Message>,
        systemInstruction: String,
        modelName: String = DEFAULT_MODEL
    ): Result<String>

    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash"
        const val PRO_MODEL = "gemini-3.1-pro-preview"
    }
}
