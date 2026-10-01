package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.example.core.model.Message
import com.example.core.model.MessageSender
import com.example.data.api.GeminiApi
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class GeminiApiKeyMissingException(message: String) : Exception(message)

/**
 * Production-ready Gemini Service implementation using Retrofit and OkHttp.
 * Adheres strictly to Gemini API guidelines (60s timeouts, safe background threading).
 */
class GeminiServiceImpl(
    private val injectedApiKey: String = BuildConfig.GEMINI_API_KEY
) : GeminiService {

    private val tag = "GeminiService"
    private val baseUrl = "https://generativelanguage.googleapis.com/"

    override val isApiKeyConfigured: Boolean
        get() = injectedApiKey.isNotBlank() && injectedApiKey != "MY_GEMINI_API_KEY"

    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApi::class.java)
    }

    override suspend fun generateResponse(
        userMessage: String,
        conversationHistory: List<Message>,
        systemInstruction: String,
        modelName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext Result.failure(
                GeminiApiKeyMissingException(
                    "Gemini API key is not configured. Please open the Secrets panel in AI Studio or add GEMINI_API_KEY to your .env file."
                )
            )
        }

        try {
            // Build conversation turns from history (keep last 8 turns for responsive voice latency)
            val turns = mutableListOf<GeminiContent>()

            val relevantHistory = conversationHistory
                .filter { it.sender == MessageSender.USER || it.sender == MessageSender.JARVIS }
                .takeLast(8)

            for (msg in relevantHistory) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                turns.add(
                    GeminiContent(
                        role = role,
                        parts = listOf(GeminiPart(text = msg.text))
                    )
                )
            }

            // Append current user message
            turns.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userMessage))
                )
            )

            val request = GeminiRequest(
                contents = turns,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstruction))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.6f,
                    topP = 0.95f,
                    topK = 40,
                    maxOutputTokens = 800 // Spoken voice responses should remain crisp & concise
                )
            )

            val response = api.generateContent(
                model = modelName,
                apiKey = injectedApiKey,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val candidateText = body?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                if (!candidateText.isNullOrBlank()) {
                    Result.success(candidateText.trim())
                } else {
                    Result.failure(Exception("Gemini returned an empty response candidate."))
                }
                    val errorCode = response.code
        val errorBody = response.message
        Log.e(tag, "Gemini API HTTP Error $errorCode: $errorBody")

                val friendlyMessage = when (errorCode) {
                    400 -> "Request format was invalid ($errorCode)."
                    401, 403 -> "Invalid or unauthorized Gemini API key. Please check your key."
                    429 -> "Gemini API rate limit exceeded. Please wait a moment."
                    500, 503 -> "Google Gemini service is temporarily unavailable. Please retry shortly."
                    else -> "Gemini API error ($errorCode)."
                }
                Result.failure(Exception(friendlyMessage))
            }
        } catch (e: UnknownHostException) {
            Log.e(tag, "Network unavailable", e)
            Result.failure(IOException("No network connection. Please check your internet connection.", e))
        } catch (e: SocketTimeoutException) {
            Log.e(tag, "Network timeout", e)
            Result.failure(IOException("Connection to Gemini timed out. Please try again.", e))
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error calling Gemini API", e)
            Result.failure(e)
        }
    }
}
