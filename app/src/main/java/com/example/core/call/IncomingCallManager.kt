package com.example.core.call

import android.content.Context

object IncomingCallManager {
    var isCallActive = false
        private set
    private var incomingNumber: String? = null
    private var appContext: Context? = null

    fun onIncomingCall(context: Context, number: String) {
        isCallActive = true
        incomingNumber = number
        appContext = context.applicationContext
    }

    fun handleVoiceCommand(command: String): Boolean {
        if (!isCallActive) return false
        val lower = command.lowercase()
        val ctx = appContext ?: return false

        if (lower.contains("uthao") || lower.contains("utha") || lower.contains("answer") || lower.contains("pick") || lower.contains("receive") || lower.contains("haan") || lower.contains("yes")) {
            CallActionHandler.answerCall(ctx)
            clearCall()
            return true
        }
        if (lower.contains("kaat") || lower.contains("reject") || lower.contains("cut") || lower.contains("decline") || lower.contains("nahi") || lower.contains("no") || lower.contains("cancel")) {
            CallActionHandler.rejectCall(ctx)
            clearCall()
            return true
        }
        return false
    }

    fun clearCall() {
        isCallActive = false
        incomingNumber = null
    }

    fun getIncomingNumber(): String? = incomingNumber
}
