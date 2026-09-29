package com.example.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

data class PermissionState(
    val hasAudioPermission: Boolean,
    val isSpeechRecognitionAvailable: Boolean,
    val isNetworkConnected: Boolean,
    val hasContactsPermission: Boolean = false,
    val hasCallPhonePermission: Boolean = false
) {
    val isFullyOperational: Boolean
        get() = hasAudioPermission && isSpeechRecognitionAvailable && isNetworkConnected
}

/**
 * Checks system permissions and hardware availability for JARVIS.
 */
class PermissionManager(private val context: Context) {

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun isNetworkConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getComprehensiveState(): PermissionState {
        return PermissionState(
            hasAudioPermission = hasRecordAudioPermission(),
            isSpeechRecognitionAvailable = isSpeechRecognitionAvailable(),
            isNetworkConnected = isNetworkConnected(),
            hasContactsPermission = hasContactsPermission(),
            hasCallPhonePermission = hasCallPhonePermission()
        )
    }
}
