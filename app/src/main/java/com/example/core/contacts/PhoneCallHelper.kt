package com.example.core.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat

object PhoneCallHelper {

    private const val TAG = "PhoneCallHelper"

    sealed interface CallResult {
        object CallInitiated : CallResult
        object DialerOpened : CallResult
        data class Failure(val reason: String) : CallResult
    }

    /**
     * Initiates an outgoing call to the specified phone number.
     * Uses ACTION_CALL if CALL_PHONE permission is granted, otherwise falls back to ACTION_DIAL.
     */
    fun makeCall(context: Context, rawNumber: String): CallResult {
        val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.length < 3) {
            return CallResult.Failure("Invalid telephone number provided.")
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            if (hasCallPermission) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                Log.d(TAG, "Direct call placed via ACTION_CALL to $cleanNumber")
                CallResult.CallInitiated
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                Log.d(TAG, "Dialer opened via ACTION_DIAL for $cleanNumber")
                CallResult.DialerOpened
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException initiating call, falling back to dialer", e)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                CallResult.DialerOpened
            } catch (ex: Exception) {
                CallResult.Failure("Unable to place call or launch dialer: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating outgoing call", e)
            CallResult.Failure("Call execution error: ${e.message}")
        }
    }
}
