package com.example.core.call

import android.telecom.Call
import android.telecom.CallScreeningService

class CallScreeningServiceImpl : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
            IncomingCallManager.onIncomingCall(this, number)
        }
        val response = CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSkipCallLog(false).setSkipNotification(false).build()
        respondToCall(callDetails, response)
    }
}
