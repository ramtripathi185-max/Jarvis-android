package com.example.core.voice.live

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.core.model.LanguageOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class LiveSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

interface GeminiLiveSession {
    val state: StateFlow<LiveSessionState>

    fun connect(
        language: LanguageOption,
        systemInstruction: String,
        voiceName: String = "Puck",
        onAudioChunk: (ByteArray) -> Unit,
        onTextToken: (String) -> Unit,
        onTurnComplete: () -> Unit,
        onInterrupted: () -> Unit,
        onError: (String) -> Unit
    )

    fun sendAudioChunk(pcmBytes: ByteArray)
    fun sendTextMessage(text: String)
    fun disconnect()
}

class OkHttpGeminiLiveSession(
    private val injectedApiKey: String = BuildConfig.GEMINI_API_KEY
) : GeminiLiveSession {

    private val tag = "GeminiLiveSession"

    private val _state = MutableStateFlow(LiveSessionState.DISCONNECTED)
    override val state: StateFlow<LiveSessionState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for WebSocket
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var onAudioChunkCallback: ((ByteArray) -> Unit)? = null
    private var onTextTokenCallback: ((String) -> Unit)? = null
    private var onTurnCompleteCallback: (() -> Unit)? = null
    private var onInterruptedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    // Target model for Gemini Real-time Multimodal Live Audio
    private val liveAudioModel = "models/gemini-2.5-flash-native-audio-preview-12-2025"

    override fun connect(
        language: LanguageOption,
        systemInstruction: String,
        voiceName: String,
        onAudioChunk: (ByteArray) -> Unit,
        onTextToken: (String) -> Unit,
        onTurnComplete: () -> Unit,
        onInterrupted: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_state.value == LiveSessionState.CONNECTING || _state.value == LiveSessionState.CONNECTED) {
            return
        }

        if (injectedApiKey.isBlank() || injectedApiKey == "MY_GEMINI_API_KEY") {
            _state.value = LiveSessionState.ERROR
            onError("Gemini API key is not configured. Please add your GEMINI_API_KEY in AI Studio.")
            return
        }

        onAudioChunkCallback = onAudioChunk
        onTextTokenCallback = onTextToken
        onTurnCompleteCallback = onTurnComplete
        onInterruptedCallback = onInterrupted
        onErrorCallback = onError

        _state.value = LiveSessionState.CONNECTING

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$injectedApiKey"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Gemini Live WebSocket session opened.")
                _state.value = LiveSessionState.CONNECTED

                // Send setup message
                val setupPayload = buildSetupJson(language, systemInstruction, voiceName)
                webSocket.send(setupPayload)
                _state.value = LiveSessionState.LISTENING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Gemini Live WebSocket closing: $code / $reason")
                _state.value = LiveSessionState.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Gemini Live WebSocket closed.")
                _state.value = LiveSessionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "Gemini Live WebSocket failure: ${t.message}", t)
                _state.value = LiveSessionState.ERROR
                val message = when {
                    t.message?.contains("403") == true -> "Gemini API key is unauthorized or lacks Live API preview access."
                    t.message?.contains("Unable to resolve host") == true -> "No internet connection to Gemini servers."
                    else -> "Live session connection error: ${t.message ?: "Unknown error"}"
                }
                onErrorCallback?.invoke(message)
            }
        })
    }

    override fun sendAudioChunk(pcmBytes: ByteArray) {
        if (webSocket == null || _state.value == LiveSessionState.DISCONNECTED || _state.value == LiveSessionState.ERROR) {
            return
        }

        try {
            val base64Data = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
            val realtimeInput = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        })
                    })
                })
            }
            webSocket?.send(realtimeInput.toString())
        } catch (e: Exception) {
            Log.w(tag, "Error sending realtime audio chunk", e)
        }
    }

    override fun sendTextMessage(text: String) {
        if (webSocket == null) return
        try {
            val clientContent = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", text)
                                })
                            })
                        })
                    })
                    put("turnComplete", true)
                })
            }
            webSocket?.send(clientContent.toString())
            _state.value = LiveSessionState.THINKING
        } catch (e: Exception) {
            Log.e(tag, "Error sending text message over WebSocket", e)
        }
    }

    override fun disconnect() {
        try {
            webSocket?.close(1000, "Client initiated disconnect")
        } catch (e: Exception) {
            Log.w(tag, "Error closing WebSocket", e)
        } finally {
            webSocket = null
            _state.value = LiveSessionState.DISCONNECTED
        }
    }

    private fun handleServerMessage(jsonString: String) {
        try {
            val root = JSONObject(jsonString)

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                // 1. Interruption notice
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(tag, "Server interrupted model output.")
                    _state.value = LiveSessionState.INTERRUPTED
                    onInterruptedCallback?.invoke()
                    _state.value = LiveSessionState.LISTENING
                    return
                }

                // 2. Model turn audio & text parts
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Check audio inline data
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Data = inlineData.optString("data")
                                if (base64Data.isNotBlank()) {
                                    val pcmAudio = Base64.decode(base64Data, Base64.NO_WRAP)
                                    _state.value = LiveSessionState.SPEAKING
                                    onAudioChunkCallback?.invoke(pcmAudio)
                                }
                            }

                            // Check transcript text
                            if (part.has("text")) {
                                val text = part.optString("text")
                                if (text.isNotBlank()) {
                                    onTextTokenCallback?.invoke(text)
                                }
                            }
                        }
                    }
                }

                // 3. Turn complete notice
                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d(tag, "Model turn complete.")
                    _state.value = LiveSessionState.LISTENING
                    onTurnCompleteCallback?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error parsing server WebSocket message", e)
        }
    }

    private fun buildSetupJson(
        language: LanguageOption,
        systemInstruction: String,
        voiceName: String
    ): String {
        val root = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", liveAudioModel)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            })
        }
        return root.toString()
    }
}
