package com.example.core.voice

import com.example.core.model.LanguageOption
import kotlinx.coroutines.flow.StateFlow

/**
 * Pluggable Voice Engine interface.
 * In Part 1, this is powered by Android's SpeechRecognizer and TextToSpeech.
 * In Part 2, a GeminiLiveVoiceEngine or Bidirectional Audio Engine can implement this
 * interface seamlessly.
 */
interface VoiceEngine {
    val isListening: StateFlow<Boolean>
    val isSpeaking: StateFlow<Boolean>
    val audioLevel: StateFlow<Float> // Normalized RMS 0.0f .. 1.0f for circular visualizer
    val currentLanguage: StateFlow<LanguageOption>

    fun setLanguage(language: LanguageOption)
    fun setTtsSpeechRate(rate: Float)
    fun setTtsPitch(pitch: Float)

    /**
     * Starts listening for user voice speech.
     */
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null
    )

    /**
     * Stops listening.
     */
    fun stopListening()

    /**
     * Synthesizes and speaks text out loud.
     */
    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    )

    /**
     * Immediately stops any active speech output.
     */
    fun stopSpeaking()

    /**
     * Releases speech resources.
     */
    fun shutdown()
}
