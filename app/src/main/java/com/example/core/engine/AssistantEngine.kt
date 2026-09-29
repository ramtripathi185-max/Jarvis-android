package com.example.core.engine

import com.example.core.actions.ActionManager
import com.example.core.conversation.ConversationManager
import com.example.core.conversation.ConversationRepository
import com.example.core.model.AssistantState
import com.example.core.model.PendingAction
import com.example.core.voice.VoiceEngine
import com.example.data.service.GeminiService
import kotlinx.coroutines.flow.StateFlow

/**
 * Core engine interface orchestrating voice, actions, conversational AI, and safety protocols.
 */
interface AssistantEngine {
    val state: StateFlow<AssistantState>
    val voiceEngine: VoiceEngine
    val actionManager: ActionManager
    val geminiService: GeminiService
    val conversationManager: ConversationManager
    val conversationRepository: ConversationRepository

    suspend fun processUserInput(input: String, isVoice: Boolean = false)
    suspend fun resolvePendingAction(approved: Boolean)
    fun startVoiceListening(onError: (String) -> Unit)
    fun stopVoiceListening()
    fun cancelActiveOperation()
}
