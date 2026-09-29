package com.example.core.voice.live

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Interface for microphone audio capture.
 */
interface AudioInputManager {
    val isRecording: StateFlow<Boolean>
    val inputAudioLevel: StateFlow<Float> // Normalized RMS level 0.0 .. 1.0

    /**
     * Starts recording audio at 16kHz PCM 16-bit Mono.
     * @param onAudioChunk Callback receiving raw PCM audio bytes.
     * @param onSpeechDetected Callback triggered when voice energy exceeds threshold (for interruption).
     * @param onError Callback for hardware or permission errors.
     */
    fun startRecording(
        onAudioChunk: (ByteArray) -> Unit,
        onSpeechDetected: () -> Unit,
        onError: (String) -> Unit
    )

    fun stopRecording()
    fun release()
}

/**
 * Android AudioRecord implementation for capturing 16kHz 16-bit PCM audio.
 */
class DefaultAudioInputManager : AudioInputManager {

    private val tag = "DefaultAudioInputManager"

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _inputAudioLevel = MutableStateFlow(0f)
    override val inputAudioLevel: StateFlow<Float> = _inputAudioLevel.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Configuration for Gemini Live API: 16kHz, 16-bit PCM, Mono
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // Interruption energy threshold (normalized RMS)
    private val speechEnergyThreshold = 0.22f

    @SuppressLint("MissingPermission")
    override fun startRecording(
        onAudioChunk: (ByteArray) -> Unit,
        onSpeechDetected: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isRecording.value) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                onError("Failed to determine minimum audio buffer size.")
                return
            }

            // Buffer size for ~100ms chunks (1600 samples * 2 bytes = 3200 bytes)
            val chunkBufferSize = (sampleRate * 2 * 0.1).toInt().coerceAtLeast(minBufferSize)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                chunkBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                onError("Microphone hardware initialization failed.")
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch {
                val buffer = ByteArray(3200) // ~100ms PCM chunk
                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        val rms = calculateNormalizedRms(chunk)
                        _inputAudioLevel.value = rms

                        if (rms > speechEnergyThreshold) {
                            onSpeechDetected()
                        }

                        onAudioChunk(chunk)
                    } else if (read < 0) {
                        Log.w(tag, "AudioRecord read error: $read")
                        break
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Microphone permission denied", e)
            _isRecording.value = false
            onError("Microphone permission required for real-time audio.")
        } catch (e: Exception) {
            Log.e(tag, "Error starting AudioRecord", e)
            _isRecording.value = false
            onError("Failed to start audio recording: ${e.message}")
        }
    }

    override fun stopRecording() {
        _isRecording.value = false
        _inputAudioLevel.value = 0f
        recordJob?.cancel()
        recordJob = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error stopping audioRecord", e)
        }
    }

    override fun release() {
        stopRecording()
        try {
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing audioRecord", e)
        }
    }

    /**
     * Calculates normalized RMS (0.0 to 1.0) from 16-bit PCM buffer.
     */
    private fun calculateNormalizedRms(pcmBytes: ByteArray): Float {
        if (pcmBytes.isEmpty()) return 0f
        var sumSquares = 0.0
        val numSamples = pcmBytes.size / 2
        for (i in 0 until numSamples) {
            val sample = (pcmBytes[i * 2 + 1].toInt() shl 8) or (pcmBytes[i * 2].toInt() and 0xFF)
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / numSamples)
        // Max 16-bit sample is 32767. Normalize and scale logarithmically
        val normalized = (rms / 32767.0).toFloat() * 4.5f
        return normalized.coerceIn(0f, 1f)
    }
}
