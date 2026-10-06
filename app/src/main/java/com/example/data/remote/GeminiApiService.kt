package com.example.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = 0.85f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40,
    val maxOutputTokens: Int? = 320
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiNetworkClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Calls Gemini with the preferred model, and automatically falls back to alternative
     * current models if the user's API key/tier doesn't expose a specific preview alias.
     */
    suspend fun generateWithFallback(
        preferredModel: String,
        apiKey: String,
        request: GenerateContentRequest
    ): GenerateContentResponse {
        val candidateModels = listOf(
            preferredModel.ifBlank { "gemini-3.5-flash" },
            "gemini-3.5-flash",
            "gemini-flash-latest",
            "gemini-3.1-flash-lite-preview"
        ).distinct()

        var lastException: Exception? = null
        for (model in candidateModels) {
            try {
                return service.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )
            } catch (e: HttpException) {
                lastException = e
                // If 404 (model not found on user's key) or 400, try next compatible model
                if (e.code() == 404 || e.code() == 400) {
                    continue
                } else {
                    throw e
                }
            } catch (e: Exception) {
                lastException = e
                throw e
            }
        }
        throw lastException ?: IllegalStateException("Gagal menghubungi layanan Gemini.")
    }
}
