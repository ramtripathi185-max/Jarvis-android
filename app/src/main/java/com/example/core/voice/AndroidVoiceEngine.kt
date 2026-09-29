package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.core.model.LanguageOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Android implementation of VoiceEngine using SpeechRecognizer and TextToSpeech.
 * Fully modular and decoupled so it can be swapped with GeminiLiveVoiceEngine in Part 2.
 */
class AndroidVoiceEngine(
    private val context: Context,
    initialLanguage: LanguageOption = LanguageOption.ENGLISH_INDIA
) : VoiceEngine {

    private val tag = "AndroidVoiceEngine"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    override val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _currentLanguage = MutableStateFlow(initialLanguage)
    override val currentLanguage: StateFlow<LanguageOption> = _currentLanguage.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechRate: Float = 1.0f
    private var pendingPitch: Float = 1.0f

    private var activeSpeechOnResult: ((String) -> Unit)? = null
    private var activeSpeechOnError: ((String) -> Unit)? = null
    private var activeSpeechOnPartial: ((String) -> Unit)? = null

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                applyTtsLocale(_currentLanguage.value)
                textToSpeech?.setSpeechRate(pendingSpeechRate)
                textToSpeech?.setPitch(pendingPitch)
                Log.d(tag, "TTS initialized successfully.")
            } else {
                Log.e(tag, "Failed to initialize TTS. Status: $status")
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.e(tag, "TTS Error code: $errorCode for utterance: $utteranceId")
            }
        })
    }

    private fun applyTtsLocale(language: LanguageOption) {
        if (!isTtsInitialized || textToSpeech == null) return
        val targetLocale = when (language) {
            LanguageOption.HINDI -> Locale.forLanguageTag("hi-IN")
            LanguageOption.HINGLISH -> Locale.forLanguageTag("hi-IN") // Hindi engine speaks Hinglish smoothly
            LanguageOption.ENGLISH_INDIA -> Locale.forLanguageTag("en-IN")
            LanguageOption.ENGLISH -> Locale.US
        }

        val result = textToSpeech?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default
            textToSpeech?.setLanguage(Locale.getDefault())
        }
    }

    override fun setLanguage(language: LanguageOption) {
        _currentLanguage.value = language
        applyTtsLocale(language)
    }

    override fun setTtsSpeechRate(rate: Float) {
        pendingSpeechRate = rate
        if (isTtsInitialized) {
            textToSpeech?.setSpeechRate(rate)
        }
    }

    override fun setTtsPitch(pitch: Float) {
        pendingPitch = pitch
        if (isTtsInitialized) {
            textToSpeech?.setPitch(pitch)
        }
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?
    ) {
        mainHandler.post {
            // Stop any ongoing speech playback
            stopSpeaking()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Speech recognition service is not available on this device.")
                return@post
            }

            activeSpeechOnResult = onResult
            activeSpeechOnError = onError
            activeSpeechOnPartial = onPartialResult

            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                } else {
                    speechRecognizer?.cancel()
                }

                speechRecognizer?.setRecognitionListener(createRecognitionListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                    val languageTag = _currentLanguage.value.tag
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _audioLevel.value = 0f
            } catch (e: Exception) {
                _isListening.value = false
                Log.e(tag, "Error starting speech recognition", e)
                onError("Failed to initiate voice input: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(tag, "Exception during stopListening", e)
            } finally {
                _isListening.value = false
                _audioLevel.value = 0f
            }
        }
    }

    override fun speak(
        text: String,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        // Strip markdown formatting symbols like asterisks for clean spoken audio
        val cleanSpeech = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}.*?`{1,3}"), "")
            .replace(Regex("#+\\s"), "")
            .trim()

        if (cleanSpeech.isBlank()) {
            onDone?.invoke()
            return
        }

        mainHandler.post {
            if (!isTtsInitialized || textToSpeech == null) {
                if (textToSpeech == null) {
                    initTextToSpeech()
                }
                onError?.invoke("Text-to-speech engine initializing. Please try again.")
                return@post
            }

            val utteranceId = UUID.randomUUID().toString()

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    if (id == utteranceId) {
                        _isSpeaking.value = true
                        mainHandler.post { onStart?.invoke() }
                    }
                }

                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        _isSpeaking.value = false
                        mainHandler.post { onDone?.invoke() }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId) {
                        _isSpeaking.value = false
                        mainHandler.post { onError?.invoke("Speech synthesis failed") }
                    }
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId) {
                        _isSpeaking.value = false
                        mainHandler.post { onError?.invoke("Speech synthesis failed (error $errorCode)") }
                    }
                }
            })

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    override fun stopSpeaking() {
        mainHandler.post {
            try {
                textToSpeech?.stop()
            } catch (e: Exception) {
                Log.w(tag, "Exception stopping TTS", e)
            } finally {
                _isSpeaking.value = false
            }
        }
    }

    override fun shutdown() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(tag, "Error destroying speechRecognizer", e)
            }

            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (e: Exception) {
                Log.w(tag, "Error shutting down TTS", e)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Android RMS dB typically ranges from -2.0 to 10.0
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _audioLevel.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _audioLevel.value = 0f
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error in speech service"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input heard"
                else -> "Speech recognition error ($error)"
            }
            activeSpeechOnError?.invoke(errorMessage)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _audioLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
            if (recognizedText.isNotBlank()) {
                activeSpeechOnResult?.invoke(recognizedText)
            } else {
                activeSpeechOnError?.invoke("Could not understand speech input")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotBlank()) {
                activeSpeechOnPartial?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
