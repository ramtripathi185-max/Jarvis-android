package com.example.core.voice.live

import android.content.Context
import android.util.Log
import com.example.core.model.LanguageOption
import com.example.core.voice.VoiceEngine
import com.example.core.voice.service.JarvisLiveAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dedicated Real-Time Voice Engine for JARVIS (Part 2A).
 * Powered by Gemini Multimodal Live API, PCM streaming AudioTrack, and AudioRecord.
 * Fully supports real-time streaming, barge-in (interruption), and Hindi/English/Hinglish.
 */
class LiveVoiceEngine(
    private val context: Context,
    private val audioInputManager: AudioInputManager = DefaultAudioInputManager(),
    private val audioOutputManager: AudioOutputManager = DefaultAudioOutputManager(),
    private val liveSession: GeminiLiveSession = OkHttpGeminiLiveSession(),
    private val fallbackVoiceEngine: VoiceEngine? = null,
    initialLanguage: LanguageOption = LanguageOption.ENGLISH_INDIA
) : VoiceEngine {

    private val tag = "LiveVoiceEngine"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    override val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _currentLanguage = MutableStateFlow(initialLanguage)
    override val currentLanguage: StateFlow<LanguageOption> = _currentLanguage.asStateFlow()

    private var speechRate: Float = 1.0f
    private var pitch: Float = 1.0f
    private var isLiveSessionActive = false

    private var activeOnResult: ((String) -> Unit)? = null
    private var activeOnError: ((String) -> Unit)? = null
    private var accumulatedTranscript = StringBuilder()

    private var monitoringJob: Job? = null

    init {
        // Synchronize audio levels to UI ArcReactor:
        // When listening -> use microphone RMS
        // When speaking -> use AudioTrack playback RMS
        monitoringJob = scope.launch {
            launch {
                audioInputManager.inputAudioLevel.collect { level ->
                    if (_isListening.value && !_isSpeaking.value) {
                        _audioLevel.value = level
                    }
                }
            }
            launch {
                audioOutputManager.outputAudioLevel.collect { level ->
                    if (_isSpeaking.value) {
                        _audioLevel.value = level
                    }
                }
            }
            launch {
                audioOutputManager.isPlaying.collect { playing ->
                    _isSpeaking.value = playing
                    if (!playing && !_isListening.value) {
                        _audioLevel.value = 0f
                    }
                }
            }
        }
    }

    override fun setLanguage(language: LanguageOption) {
        _currentLanguage.value = language
        fallbackVoiceEngine?.setLanguage(language)
    }

    override fun setTtsSpeechRate(rate: Float) {
        speechRate = rate
        fallbackVoiceEngine?.setTtsSpeechRate(rate)
    }

    override fun setTtsPitch(pitch: Float) {
        this.pitch = pitch
        fallbackVoiceEngine?.setTtsPitch(pitch)
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?
    ) {
        stopSpeaking()
        activeOnResult = onResult
        activeOnError = onError
        accumulatedTranscript.clear()

        // 1. Start Android Foreground Service for compliant background mic execution
        try {
            JarvisLiveAudioService.startService(context)
        } catch (e: Exception) {
            Log.w(tag, "Could not start JarvisLiveAudioService", e)
        }

        // 2. Connect Gemini Live Session if not connected
        val systemPrompt = buildLiveSystemInstruction(_currentLanguage.value)
        liveSession.connect(
            language = _currentLanguage.value,
            systemInstruction = systemPrompt,
            voiceName = "Puck",
            onAudioChunk = { pcmChunk ->
                // Stream audio chunk directly to AudioTrack for zero-latency playback!
                audioOutputManager.writeChunk(pcmChunk)
            },
            onTextToken = { token ->
                accumulatedTranscript.append(token)
                onPartialResult?.invoke(accumulatedTranscript.toString())
            },
            onTurnComplete = {
                val fullText = accumulatedTranscript.toString().trim()
                if (fullText.isNotBlank()) {
                    activeOnResult?.invoke(fullText)
                }
            },
            onInterrupted = {
                // Server confirmed model was interrupted
                Log.d(tag, "Interruption signal received from server.")
                audioOutputManager.flushAndStop()
                _isSpeaking.value = false
            },
            onError = { errMsg ->
                Log.w(tag, "Gemini Live error: $errMsg. Attempting fallback...")
                stopListening()
                if (fallbackVoiceEngine != null) {
                    fallbackVoiceEngine.startListening(onResult, onError, onPartialResult)
                } else {
                    onError(errMsg)
                }
            }
        )

        // 3. Start capturing mic audio and streaming PCM chunks to Gemini
        audioInputManager.startRecording(
            onAudioChunk = { pcmChunk ->
                liveSession.sendAudioChunk(pcmChunk)
            },
            onSpeechDetected = {
                // Local barge-in detection: if user speaks while JARVIS is talking, instantly cut playback
                if (_isSpeaking.value) {
                    Log.d(tag, "Local user voice energy detected during playback. Interrupting!")
                    audioOutputManager.flushAndStop()
                    _isSpeaking.value = false
                }
            },
            onError = { err ->
                Log.e(tag, "Audio input error: $err")
                stopListening()
                onError(err)
            }
        )

        _isListening.value = true
        isLiveSessionActive = true
    }

    override fun stopListening() {
        _isListening.value = false
        isLiveSessionActive = false
        audioInputManager.stopRecording()

        try {
            JarvisLiveAudioService.stopService(context)
        } catch (e: Exception) {
            Log.w(tag, "Error stopping JarvisLiveAudioService", e)
        }
    }

    override fun speak(
        text: String,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        // If Live Session is open, send as text turn to get streaming 24kHz audio from Gemini
        if (isLiveSessionActive) {
            liveSession.sendTextMessage(text)
            onStart?.invoke()
        } else if (fallbackVoiceEngine != null) {
            // Fallback to local TTS engine
            fallbackVoiceEngine.speak(text, onStart, onDone, onError)
        } else {
            onError?.invoke("No active speech synthesis channel available.")
        }
    }

    override fun stopSpeaking() {
        audioOutputManager.flushAndStop()
        _isSpeaking.value = false
        fallbackVoiceEngine?.stopSpeaking()
    }

    override fun shutdown() {
        stopListening()
        stopSpeaking()
        monitoringJob?.cancel()
        audioInputManager.release()
        audioOutputManager.release()
        liveSession.disconnect()
        fallbackVoiceEngine?.shutdown()
    }

    private fun buildLiveSystemInstruction(language: LanguageOption): String {
        val base = """
            You are J.A.R.V.I.S., a real-time voice-first personal AI assistant.
            You are speaking directly to the user over a bidirectional low-latency audio channel.
            Personality attributes:
            - Calm, intelligent, concise, polite, and respectful (e.g., 'sir', 'certainly', 'right away').
            - Speak naturally in complete, succinct sentences. Keep responses brief (1-3 sentences) so the conversation flows seamlessly.
            - Never output markdown, asterisks, bullet lists, or code formatting because your output is streamed directly to speech.
        """.trimIndent()

        val langRule = when (language) {
            LanguageOption.HINDI -> "Language requirement: Speak in elegant, natural, respectful Hindi."
            LanguageOption.HINGLISH -> "Language requirement: Speak in natural, modern conversational Hinglish (blend of Hindi and English written in Latin script), polite and fluid."
            LanguageOption.ENGLISH, LanguageOption.ENGLISH_INDIA -> "Language requirement: Speak in articulate, clean English."
        }

        return "$base\n$langRule"
    }
}
