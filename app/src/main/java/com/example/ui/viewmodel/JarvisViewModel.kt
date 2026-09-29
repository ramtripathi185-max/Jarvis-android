package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.engine.AssistantEngine
import com.example.core.model.AssistantState
import com.example.core.model.LanguageOption
import com.example.core.model.Message
import com.example.core.model.PendingAction
import com.example.core.permissions.PermissionManager
import com.example.core.permissions.PermissionState
import com.example.data.preferences.JarvisPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(
    val engine: AssistantEngine,
    val permissionManager: PermissionManager,
    val preferences: JarvisPreferences
) : ViewModel() {

    val assistantState: StateFlow<AssistantState> = engine.state
    val pendingAction: StateFlow<PendingAction?> = engine.actionManager.pendingAction

    val messages: StateFlow<List<Message>> = engine.conversationRepository
        .getMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _permissionState = MutableStateFlow(permissionManager.getComprehensiveState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    val selectedLanguage = preferences.language
    val selectedModel = preferences.modelName
    val speechRate = preferences.speechRate
    val pitch = preferences.pitch
    val autoSpeak = preferences.autoSpeak
    val customTone = preferences.customTone
    val liveVoiceMode = preferences.liveVoiceMode
    val liveVoiceName = preferences.liveVoiceName

    val isApiKeyConfigured: Boolean
        get() = engine.geminiService.isApiKeyConfigured

    init {
        // Initialize voice engine settings from preferences
        engine.voiceEngine.setLanguage(preferences.language.value)
        engine.voiceEngine.setTtsSpeechRate(preferences.speechRate.value)
        engine.voiceEngine.setTtsPitch(preferences.pitch.value)

        // Seed initial welcome message if conversation is empty
        viewModelScope.launch {
            engine.conversationRepository.seedWelcomeIfNeeded()
        }
    }

    fun onVoiceMicClick(onError: (String) -> Unit) {
        val current = assistantState.value
        if (current is AssistantState.Listening) {
            engine.stopVoiceListening()
        } else if (current is AssistantState.Speaking) {
            engine.voiceEngine.stopSpeaking()
        } else {
            if (!permissionManager.hasRecordAudioPermission()) {
                onError("Microphone permission is required to listen.")
                return
            }
            engine.startVoiceListening(onError)
        }
    }

    fun onSendTextMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            engine.processUserInput(text, isVoice = false)
        }
    }

    fun onResolvePendingAction(approved: Boolean) {
        viewModelScope.launch {
            engine.resolvePendingAction(approved)
        }
    }

    fun onCancelOperation() {
        engine.cancelActiveOperation()
    }

    fun onClearConversation() {
        viewModelScope.launch {
            engine.conversationRepository.clearHistory()
        }
    }

    fun onLanguageChanged(language: LanguageOption) {
        preferences.setLanguage(language)
        engine.voiceEngine.setLanguage(language)
    }

    fun onModelChanged(model: String) {
        preferences.setModelName(model)
    }

    fun onSpeechRateChanged(rate: Float) {
        preferences.setSpeechRate(rate)
        engine.voiceEngine.setTtsSpeechRate(rate)
    }

    fun onPitchChanged(pitchValue: Float) {
        preferences.setPitch(pitchValue)
        engine.voiceEngine.setTtsPitch(pitchValue)
    }

    fun onAutoSpeakChanged(enabled: Boolean) {
        preferences.setAutoSpeak(enabled)
    }

    fun onCustomToneChanged(tone: String) {
        preferences.setCustomTone(tone)
    }

    fun onLiveVoiceModeChanged(enabled: Boolean) {
        preferences.setLiveVoiceMode(enabled)
    }

    fun onLiveVoiceNameChanged(voiceName: String) {
        preferences.setLiveVoiceName(voiceName)
    }

    fun testTtsVoice() {
        val testPhrase = when (preferences.language.value) {
            LanguageOption.HINDI -> "नमस्ते, मैं जार्विस हूँ। सभी प्रणालियाँ सुचारू रूप से कार्य कर रही हैं।"
            LanguageOption.HINGLISH -> "Hello sir, main JARVIS hoon. All core systems are calibrated and operational."
            LanguageOption.ENGLISH_INDIA, LanguageOption.ENGLISH ->
                "Greetings sir. JARVIS speech synthesis is functioning at optimal parameters."
        }
        engine.voiceEngine.speak(testPhrase)
    }

    fun refreshPermissions() {
        _permissionState.value = permissionManager.getComprehensiveState()
    }

    override fun onCleared() {
        super.onCleared()
        engine.voiceEngine.shutdown()
    }
}

class JarvisViewModelFactory(
    private val engine: AssistantEngine,
    private val permissionManager: PermissionManager,
    private val preferences: JarvisPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JarvisViewModel::class.java)) {
            return JarvisViewModel(engine, permissionManager, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
