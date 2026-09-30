package com.example.core.call

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object IncomingCallManager {
    var currentNumber: String? = null
    private var tts: TextToSpeech? = null

    fun onIncomingCall(context: Context, number: String) {
        currentNumber = number
        speakIncomingCall(context, number)
    }

    private fun speakIncomingCall(context: Context, number: String) {
        try {
            if (tts == null) {
                tts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.language = Locale("hi", "IN")
                        tts?.speak("$number se call aa raha hai. Uthau ya kaat du?", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            } else {
                tts?.speak("$number se call aa raha hai. Uthau ya kaat du?", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } catch (e: Exception) { }
    }

    fun handleVoiceDecision(context: Context, command: String): Boolean {
        val cmd = command.lowercase()
        return when {
            cmd.contains("utha") || cmd.contains("receive") || cmd.contains("haan") || cmd.contains("yes") -> {
                CallActionHandler.answerCall(context); true
            }
            cmd.contains("kaat") || cmd.contains("cut") || cmd.contains("reject") || cmd.contains("nahi") -> {
                CallActionHandler.rejectCall(context); true
            }
            else -> false
        }
    }
}
