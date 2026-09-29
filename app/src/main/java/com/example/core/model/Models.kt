package com.example.core.model

/**
 * High-level state of the JARVIS assistant.
 */
sealed interface AssistantState {
    object Idle : AssistantState
    data class Listening(val audioLevel: Float = 0f, val partialText: String = "") : AssistantState
    data class Thinking(val query: String = "") : AssistantState
    data class Speaking(val speechText: String = "", val audioLevel: Float = 0f) : AssistantState
    data class Error(val message: String, val canRetry: Boolean = true) : AssistantState
}

/**
 * Message sender identifier.
 */
enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

/**
 * Status of an action requiring user safety confirmation or execution.
 */
enum class ActionStatus {
    PENDING_CONFIRMATION,
    EXECUTING,
    COMPLETED,
    CANCELLED,
    FAILED
}

/**
 * Pending action that requires user confirmation before execution.
 */
data class PendingAction(
    val actionId: String,
    val title: String,
    val description: String,
    val confirmationPrompt: String,
    val parameters: Map<String, String> = emptyMap(),
    val status: ActionStatus = ActionStatus.PENDING_CONFIRMATION,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Chat and voice conversation message entity.
 */
data class Message(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val pendingAction: PendingAction? = null
)

/**
 * Supported voice and conversation languages.
 */
enum class LanguageOption(val tag: String, val displayName: String, val defaultLocaleCode: String) {
    ENGLISH("en-US", "English (Global)", "en-US"),
    ENGLISH_INDIA("en-IN", "English (India)", "en-IN"),
    HINDI("hi-IN", "हिंदी (Hindi)", "hi-IN"),
    HINGLISH("en-IN", "Hinglish (Hindi + English)", "en-IN")
}
