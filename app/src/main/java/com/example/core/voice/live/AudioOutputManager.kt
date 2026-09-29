package com.example.core.voice.live

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Interface for streaming audio playback.
 */
interface AudioOutputManager {
    val isPlaying: StateFlow<Boolean>
    val outputAudioLevel: StateFlow<Float> // Normalized RMS level 0.0 .. 1.0

    /**
     * Queues streaming PCM audio chunks for immediate playback.
     */
    fun writeChunk(pcmData: ByteArray)

    /**
     * Immediately stops playback and flushes buffered audio (interruption handling).
     */
    fun flushAndStop()

    fun release()
}

/**
 * AudioTrack implementation streaming 24kHz 16-bit PCM mono output from Gemini Live.
 */
class DefaultAudioOutputManager : AudioOutputManager {

    private val tag = "DefaultAudioOutputManager"

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _outputAudioLevel = MutableStateFlow(0f)
    override val outputAudioLevel: StateFlow<Float> = _outputAudioLevel.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private val chunkChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Gemini Live audio output is 24kHz, 16-bit PCM, Mono
    private val sampleRate = 24000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    init {
        initAudioTrack()
        startPlaybackConsumer()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBufferSize.coerceAtLeast(sampleRate * 2)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build()

            audioTrack = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(tag, "AudioTrack initialization failed.")
                audioTrack?.release()
                audioTrack = null
                return
            }

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(tag, "Exception initializing AudioTrack", e)
        }
    }

    private fun startPlaybackConsumer() {
        playbackJob = scope.launch {
            while (isActive) {
                val chunk = chunkChannel.receive()
                if (chunk.isNotEmpty()) {
                    if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                        initAudioTrack()
                    }

                    if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.play()
                    }

                    _isPlaying.value = true
                    val rms = calculateNormalizedRms(chunk)
                    _outputAudioLevel.value = rms

                    try {
                        audioTrack?.write(chunk, 0, chunk.size)
                    } catch (e: Exception) {
                        Log.w(tag, "Error writing PCM chunk to AudioTrack", e)
                    }
                }
            }
        }
    }

    override fun writeChunk(pcmData: ByteArray) {
        if (pcmData.isEmpty()) return
        chunkChannel.trySend(pcmData)
    }

    override fun flushAndStop() {
        // Drain any pending chunks in channel
        while (chunkChannel.tryReceive().isSuccess) {
            // Drop buffer
        }

        try {
            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.pause()
                audioTrack?.flush()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error flushing AudioTrack", e)
        } finally {
            _isPlaying.value = false
            _outputAudioLevel.value = 0f
        }
    }

    override fun release() {
        flushAndStop()
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing AudioTrack", e)
        }
    }

    private fun calculateNormalizedRms(pcmBytes: ByteArray): Float {
        if (pcmBytes.isEmpty()) return 0f
        var sumSquares = 0.0
        val numSamples = pcmBytes.size / 2
        for (i in 0 until numSamples) {
            val sample = (pcmBytes[i * 2 + 1].toInt() shl 8) or (pcmBytes[i * 2].toInt() and 0xFF)
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / numSamples)
        val normalized = (rms / 32767.0).toFloat() * 4.0f
        return normalized.coerceIn(0f, 1f)
    }
}
