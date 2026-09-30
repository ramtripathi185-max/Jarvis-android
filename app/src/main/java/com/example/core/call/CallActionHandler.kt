package com.example.core.call

import android.content.Context
import android.os.Build
import android.telecom.TelecomManager

object CallActionHandler {
    fun answerCall(context: Context) {
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecom.acceptRingingCall()
            }
        } catch (e: Exception) { }
    }
    fun rejectCall(context: Context) {
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecom.endCall()
            }
        } catch (e: Exception) { }
    }
}
