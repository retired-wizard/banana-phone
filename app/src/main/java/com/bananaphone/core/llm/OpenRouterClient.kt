package com.bananaphone.core.llm

import android.util.Log
import com.bananaphone.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val TAG = "OpenRouterClient"
private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

@Serializable
data class Message(
    val content: String
)

/**
 * Client for communicating with OpenRouter API
 */
class OpenRouterClient {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Generates HTML from user request using OpenRouter API
     * 
     * @param userRequest The user's transcribed speech request
     * @return Generated HTML string, or null on error
     */
    suspend fun generateHTML(userRequest: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.OPENROUTER_API_KEY
            val model = BuildConfig.OPENROUTER_MODEL
            
            if (apiKey.isBlank()) {
                Log.e(TAG, "OpenRouter API key is not configured")
                return@withContext Result.failure(IllegalStateException("API key not configured"))
            }
            
            val (systemMessage, userMessage) = PromptTemplate.createPrompt(userRequest)
            
            val requestBody = json.encodeToString(
                ChatCompletionRequest.serializer(),
                ChatCompletionRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage(role = "system", content = systemMessage),
                        ChatMessage(role = "user", content = userMessage)
                    )
                )
            )
            
            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            Log.d(TAG, "Sending request to OpenRouter API (model: $model)")
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "API request failed: ${response.code} - $errorBody")
                    return@withContext Result.failure(
                        IOException("API request failed: ${response.code}")
                    )
                }
                
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Received response from API")
                
                // Parse response - OpenRouter returns OpenAI-compatible format
                val jsonResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
                
                if (jsonResponse.choices.isEmpty()) {
                    Log.e(TAG, "No choices in API response")
                    return@withContext Result.failure(IOException("No choices in API response"))
                }
                
                val html = jsonResponse.choices[0].message.content.trim()
                Log.d(TAG, "Successfully extracted HTML (length: ${html.length})")
                
                Result.success(html)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating HTML", e)
            Result.failure(e)
        }
    }
}

