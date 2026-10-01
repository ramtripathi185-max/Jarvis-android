package com.example.data.service

import com.aistudio.jarvis.kxaqvt.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiServiceImpl : GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun sendMessage(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure<String>(Exception("API Key is missing"))
            }

            val json = JSONObject().apply {
                put("contents", arrayOf(
                    JSONObject().apply {
                        put("parts", arrayOf(
                            JSONObject().apply {
                                put("text", prompt)
                            }
                        ))
                    }
                ))
            }

            val body = json.toString().toMediaType("application/json".toMediaType()).let {
                json.toString().toRequestBody(it)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorCode = response.code
                val errorBody = response.message
                val friendlyMessage = "Gemini Error $errorCode: $errorBody"
                return@withContext Result.failure<String>(Exception(friendlyMessage))
            }

            val responseBody = response.body?.string() ?: ""
            if (responseBody.isBlank()) {
                return@withContext Result.failure<String>(Exception("Gemini returned empty response"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val candidateText = parts?.optJSONObject(0)?.optString("text")

            if (!candidateText.isNullOrBlank()) {
                return@withContext Result.success(candidateText.trim())
            } else {
                return@withContext Result.failure<String>(Exception("Gemini returned empty response"))
            }

        } catch (e: IOException) {
            return@withContext Result.failure<String>(IOException("No network connection. Please check your internet connection.", e))
        } catch (e: Exception) {
            return@withContext Result.failure<String>(e)
        }
    }
}
