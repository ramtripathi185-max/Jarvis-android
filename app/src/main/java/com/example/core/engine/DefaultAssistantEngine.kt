package com.example.core.engine

import android.util.Log
import com.example.core.actions.ActionManager
import com.example.core.actions.ActionResult
import com.example.core.conversation.ConversationManager
import com.example.core.conversation.ConversationRepository
import com.example.core.model.AssistantState
import com.example.core.model.LanguageOption
import com.example.core.model.Message
import com.example.core.model.MessageSender
import com.example.core.voice.VoiceEngine
import com.example.data.preferences.JarvisPreferences
import com.example.data.service.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class DefaultAssistantEngine(
    override val voiceEngine: VoiceEngine,
    override val actionManager: ActionManager,
    override val geminiService: GeminiService,
    override val conversationManager: ConversationManager,
    override val conversationRepository: ConversationRepository,
    private val preferences: JarvisPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : AssistantEngine {

    private val tag = "DefaultAssistantEngine"
    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    override val state: StateFlow<AssistantState> = _state.asStateFlow()

    private var activeJob: Job? = null

    init {
        // Monitor voice engine audio levels when listening
        scope.launch {
            voiceEngine.audioLevel.collect { level ->
                val current = _state.value
                if (current is AssistantState.Listening) {
                    _state.value = current.copy(audioLevel = level)
                } else if (current is AssistantState.Speaking) {
                    _state.value = current.copy(audioLevel = level)
                }
            }
        }
    }

    override fun startVoiceListening(onError: (String) -> Unit) {
        cancelActiveOperation()
        _state.value = AssistantState.Listening()

        voiceEngine.startListening(
            onResult = { recognizedText ->
                _state.value = AssistantState.Idle
                scope.launch {
                    processUserInput(recognizedText, isVoice = true)
                }
            },
            onError = { errorMsg ->
                _state.value = AssistantState.Idle
                Log.w(tag, "Speech recognition error: $errorMsg")
                onError(errorMsg)
            },
            onPartialResult = { partial ->
                _state.value = AssistantState.Listening(partialText = partial)
            }
        )
    }

    override fun stopVoiceListening() {
        voiceEngine.stopListening()
        if (_state.value is AssistantState.Listening) {
            _state.value = AssistantState.Idle
        }
    }

    override fun cancelActiveOperation() {
        activeJob?.cancel()
        activeJob = null
        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
        actionManager.clearPendingAction()
        _state.value = AssistantState.Idle
    }

    override suspend fun processUserInput(input: String, isVoice: Boolean) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        // Check if there is an active pending action awaiting confirmation
        val currentPending = actionManager.pendingAction.value
        if (currentPending != null) {
            val lower = trimmed.lowercase(Locale.ROOT)
            val isApproval = isAffirmativeResponse(lower)
            val isDenial = isNegativeResponse(lower)

            if (isApproval || isDenial) {
                conversationManager.recordUserMessage(trimmed, isVoice = isVoice)
                resolvePendingAction(approved = isApproval)
                return
            }
        }

        // Record user turn in dialogue history
        conversationManager.recordUserMessage(trimmed, isVoice = isVoice)

        // 1. Evaluate registered actions
        val matchingAction = actionManager.findMatchingAction(trimmed)
        if (matchingAction != null) {
            _state.value = AssistantState.Thinking("Evaluating command...")
            val actionResult = actionManager.executeAction(matchingAction, trimmed)

            when (actionResult) {
                is ActionResult.Success -> {
                    conversationManager.recordAssistantMessage(actionResult.responseMessage)
                    speakOrIdle(actionResult.responseMessage, isVoice)
                }
                is ActionResult.RequiresConfirmation -> {
                    _state.value = AssistantState.Idle
                    val prompt = actionResult.pendingAction.confirmationPrompt
                    val message = Message(
                        id = java.util.UUID.randomUUID().toString(),
                        text = prompt,
                        sender = MessageSender.JARVIS,
                        pendingAction = actionResult.pendingAction
                    )
                    conversationRepository.addMessage(message)
                    speakOrIdle(prompt, isVoice)
                }
                is ActionResult.MissingPermission -> {
                    val notice = "Permission required: ${actionResult.rationale}"
                    conversationManager.recordSystemMessage(notice)
                    _state.value = AssistantState.Error(notice)
                    speakOrIdle(notice, isVoice)
                }
                is ActionResult.Failure -> {
                    val err = "Unable to execute command: ${actionResult.errorMessage}"
                    conversationManager.recordSystemMessage(err)
                    _state.value = AssistantState.Error(err)
                    speakOrIdle(err, isVoice)
                }
            }
            return
        }

        // 2. Delegate to Google Gemini API
        activeJob?.cancel()
        activeJob = scope.launch {
            _state.value = AssistantState.Thinking(trimmed)

            val currentLang = preferences.language.value
            val currentModel = preferences.modelName.value
            val customTone = preferences.customTone.value

            val systemInstruction = conversationManager.buildSystemInstruction(currentLang, customTone)
            val history = try {
                conversationRepository.getMessages().first()
            } catch (e: Exception) {
                emptyList()
            }

            val result = geminiService.generateResponse(
                userMessage = trimmed,
                conversationHistory = history,
                systemInstruction = systemInstruction,
                modelName = currentModel
            )

            result.fold(
                onSuccess = { assistantResponse ->
                    conversationManager.recordAssistantMessage(assistantResponse)
                    speakOrIdle(assistantResponse, isVoice)
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "An unexpected error occurred."
                    Log.e(tag, "Gemini generation failure", error)
                    _state.value = AssistantState.Error(errorMsg)
                    conversationManager.recordSystemMessage("System notice: $errorMsg")
                    if (isVoice) {
                        val spokenNotice = if (currentLang == LanguageOption.HINDI) {
                            "Maaf kijiye, mujhe response generate karne mein samasya aa rahi hai."
                        } else {
                            "I apologize, I was unable to connect to the core server."
                        }
                        voiceEngine.speak(spokenNotice)
                    }
                }
            )
        }
    }

    override suspend fun resolvePendingAction(approved: Boolean) {
        _state.value = AssistantState.Thinking(if (approved) "Executing action..." else "Aborting...")
        val result = actionManager.resolvePendingAction(approved)

        when (result) {
            is ActionResult.Success -> {
                conversationManager.recordAssistantMessage(result.responseMessage)
                speakOrIdle(result.responseMessage, isVoice = true)
            }
            is ActionResult.Failure -> {
                conversationManager.recordSystemMessage(result.errorMessage)
                _state.value = AssistantState.Error(result.errorMessage)
            }
            else -> {
                _state.value = AssistantState.Idle
            }
        }
    }

    private fun speakOrIdle(text: String, isVoice: Boolean) {
        val autoSpeak = preferences.autoSpeak.value
        if (autoSpeak || isVoice) {
            _state.value = AssistantState.Speaking(speechText = text)
            voiceEngine.speak(
                text = text,
                onStart = {
                    _state.value = AssistantState.Speaking(speechText = text)
                },
                onDone = {
                    _state.value = AssistantState.Idle
                },
                onError = {
                    _state.value = AssistantState.Idle
                }
            )
        } else {
            _state.value = AssistantState.Idle
        }
    }

    private fun isAffirmativeResponse(input: String): Boolean {
        return input == "yes" || input == "yeah" || input == "sure" ||
                input == "confirm" || input == "authorize" || input == "haan" ||
                input == "theek hai" || input == "do it" || input == "proceed"
    }

    private fun isNegativeResponse(input: String): Boolean {
        return input == "no" || input == "nope" || input == "cancel" ||
                input == "abort" || input == "stop" || input == "nahi" ||
                input == "mat karo" || input == "don't"
    }
}
