package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.LanguageOption
import com.example.data.service.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<LanguageOption> = _language.asStateFlow()

    private val _modelName = MutableStateFlow(loadModelName())
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _speechRate = MutableStateFlow(loadSpeechRate())
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(loadPitch())
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _autoSpeak = MutableStateFlow(loadAutoSpeak())
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _customTone = MutableStateFlow(loadCustomTone())
    val customTone: StateFlow<String> = _customTone.asStateFlow()

    private fun loadLanguage(): LanguageOption {
        val name = prefs.getString(KEY_LANGUAGE, LanguageOption.ENGLISH_INDIA.name)
        return try {
            LanguageOption.valueOf(name ?: LanguageOption.ENGLISH_INDIA.name)
        } catch (e: Exception) {
            LanguageOption.ENGLISH_INDIA
        }
    }

    private fun loadModelName(): String {
        return prefs.getString(KEY_MODEL, GeminiService.DEFAULT_MODEL) ?: GeminiService.DEFAULT_MODEL
    }

    private fun loadSpeechRate(): Float {
        return prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
    }

    private fun loadPitch(): Float {
        return prefs.getFloat(KEY_PITCH, 1.0f)
    }

    private fun loadAutoSpeak(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SPEAK, true)
    }

    private fun loadCustomTone(): String {
        return prefs.getString(KEY_CUSTOM_TONE, "") ?: ""
    }

    fun setLanguage(option: LanguageOption) {
        prefs.edit().putString(KEY_LANGUAGE, option.name).apply()
        _language.value = option
    }

    fun setModelName(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _modelName.value = model
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _speechRate.value = rate
    }

    fun setPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_PITCH, pitch).apply()
        _pitch.value = pitch
    }

    fun setAutoSpeak(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
        _autoSpeak.value = enabled
    }

    fun setCustomTone(tone: String) {
        prefs.edit().putString(KEY_CUSTOM_TONE, tone).apply()
        _customTone.value = tone
    }

    companion object {
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_MODEL = "key_model"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_PITCH = "key_pitch"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_CUSTOM_TONE = "key_custom_tone"
    }
}
