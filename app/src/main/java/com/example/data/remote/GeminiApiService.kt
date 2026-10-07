package com.example.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
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
    val parts: List<Part>? = null
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
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null,
    val error: GeminiApiErrorDetail? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null
)

@Serializable
data class GeminiErrorEnvelope(
    val error: GeminiApiErrorDetail? = null
)

@Serializable
data class GeminiApiErrorDetail(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

sealed class GeminiRemoteException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidApiKeyException(message: String) : GeminiRemoteException(message)
    class QuotaExceededException(message: String) : GeminiRemoteException(message)
    class SafetyBlockedException(message: String) : GeminiRemoteException(message)
    class EmptyResponseException(message: String) : GeminiRemoteException(message)
    class ServerHttpException(val statusCode: Int, message: String) : GeminiRemoteException(message)
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @Streaming
    @POST("v1beta/models/{model}:streamGenerateContent")
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Query("alt") alt: String = "sse",
        @Body request: GenerateContentRequest
    ): ResponseBody
}

object GeminiNetworkClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Bounded automatic retry: maximum 2 retries (1 initial + up to 2 retries) for transient network/server errors
    private const val MAX_RETRIES = 2

    private val lastWorkingModelByPreference = ConcurrentHashMap<String, String>()

    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
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
     * Streams response chunks from Gemini API using SSE (`streamGenerateContent?alt=sse`) so text
     * appears quickly in the UI before the full generation finishes. Automatically falls back to
     * `generateContent` and across available fast models, with up to [MAX_RETRIES] retries for
     * transient network/server errors.
     */
    suspend fun streamOrGenerateWithFallback(
        preferredModel: String,
        apiKey: String,
        request: GenerateContentRequest,
        onPartialText: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val candidateModels = resolveCandidateModels(preferredModel)
        val basePreferenceKey = preferredModel.trim().ifBlank { "gemini-3.5-flash" }
        var lastException: Exception? = null

        for (model in candidateModels) {
            try {
                val resultText = executeWithBoundedRetry(
                    model = model,
                    apiKey = apiKey,
                    request = request,
                    onPartialText = onPartialText
                )
                if (resultText.isNotBlank()) {
                    lastWorkingModelByPreference[basePreferenceKey] = model
                    return@withContext resultText
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiRemoteException.InvalidApiKeyException) {
                // Do not try fallback models when the API key itself is invalid
                throw e
            } catch (e: GeminiRemoteException.QuotaExceededException) {
                lastException = e
                // Try another model tier in case quota is per-model; otherwise throw at the end
                continue
            } catch (e: GeminiRemoteException.SafetyBlockedException) {
                throw e
            } catch (e: UnknownHostException) {
                // Device is offline; fail immediately rather than cycling models
                throw e
            } catch (e: SocketTimeoutException) {
                // Retries for this model already exhausted; fail fast rather than hanging for minutes
                throw e
            } catch (e: HttpException) {
                lastException = e
                val code = extractHttpStatusCode(e)
                val errorBody = extractSafeErrorBody(e, apiKey)
                if (isApiKeyInvalidError(code, errorBody)) {
                    throw GeminiRemoteException.InvalidApiKeyException("API key tidak valid atau belum aktif.")
                }
                if (code == 404 || code == 400) {
                    // Model not found or unsupported on this endpoint -> try next candidate model
                    continue
                }
                throw GeminiRemoteException.ServerHttpException(
                    statusCode = code,
                    message = "HTTP $code"
                )
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                continue
            }
        }

        when (val ex = lastException) {
            null -> throw GeminiRemoteException.EmptyResponseException("Respons kosong dari Gemini.")
            else -> throw ex
        }
    }

    /**
     * Kept for backward compatibility; delegates to [streamOrGenerateWithFallback] or returns
     * a synthesized [GenerateContentResponse].
     */
    suspend fun generateWithFallback(
        preferredModel: String,
        apiKey: String,
        request: GenerateContentRequest
    ): GenerateContentResponse {
        val text = streamOrGenerateWithFallback(
            preferredModel = preferredModel,
            apiKey = apiKey,
            request = request
        )
        return GenerateContentResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        role = "model",
                        parts = listOf(Part(text = text))
                    )
                )
            )
        )
    }

    private fun resolveCandidateModels(preferredModel: String): List<String> {
        val base = preferredModel.trim().ifBlank { "gemini-3.5-flash" }
        val cachedWorking = lastWorkingModelByPreference[base]
        return buildList {
            if (!cachedWorking.isNullOrBlank()) {
                add(cachedWorking)
            }
            add(base)
            add("gemini-2.5-flash")
            add("gemini-flash-latest")
            add("gemini-2.0-flash")
            add("gemini-3.5-flash")
            add("gemini-3.1-flash-lite-preview")
        }.distinct()
    }

    private suspend fun executeWithBoundedRetry(
        model: String,
        apiKey: String,
        request: GenerateContentRequest,
        onPartialText: (String) -> Unit
    ): String {
        var attempt = 0
        var lastError: Exception? = null

        while (attempt <= MAX_RETRIES) {
            try {
                // 1. Try streaming SSE first for fast time-to-first-token
                val streamedText = tryStreamGenerate(
                    model = model,
                    apiKey = apiKey,
                    request = request,
                    onPartialText = onPartialText
                )
                if (streamedText.isNotBlank()) {
                    return streamedText
                }

                // 2. If stream returned empty without error, fallback to standard generateContent
                val nonStreamResponse = service.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )
                val extracted = extractTextFromResponse(nonStreamResponse)
                if (extracted.isNotBlank()) {
                    onPartialText(extracted)
                    return extracted
                }
                throw GeminiRemoteException.EmptyResponseException("Respons kosong dari model $model")
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiRemoteException.InvalidApiKeyException) {
                throw e
            } catch (e: GeminiRemoteException.SafetyBlockedException) {
                throw e
            } catch (e: HttpException) {
                val statusCode = extractHttpStatusCode(e)
                val errorBody = extractSafeErrorBody(e, apiKey)

                if (isApiKeyInvalidError(statusCode, errorBody)) {
                    throw GeminiRemoteException.InvalidApiKeyException("API key tidak valid.")
                }

                if (statusCode == 400 || statusCode == 401 || statusCode == 403 || statusCode == 404) {
                    // Client/model error is not retryable on the same model
                    throw e
                }

                lastError = if (statusCode == 429) {
                    GeminiRemoteException.QuotaExceededException("Batas kuota tercapai (429).")
                } else {
                    e
                }

                if (!isRetryableHttpStatus(statusCode) || attempt >= MAX_RETRIES) {
                    throw lastError
                }
            } catch (e: UnknownHostException) {
                lastError = e
                // Only 1 quick retry for transient DNS resolution blip
                if (attempt >= 1) {
                    throw e
                }
            } catch (e: SocketTimeoutException) {
                lastError = e
                if (attempt >= MAX_RETRIES) {
                    throw e
                }
            } catch (e: IOException) {
                lastError = e
                if (attempt >= MAX_RETRIES) {
                    throw e
                }
            } catch (e: GeminiRemoteException.EmptyResponseException) {
                lastError = e
                if (attempt >= 1) {
                    throw e
                }
            }

            attempt++
            val backoffMs = (600L * attempt).coerceAtMost(1800L)
            delay(backoffMs)
        }

        throw lastError ?: GeminiRemoteException.EmptyResponseException("Gagal memperoleh respons dari Gemini.")
    }

    private suspend fun tryStreamGenerate(
        model: String,
        apiKey: String,
        request: GenerateContentRequest,
        onPartialText: (String) -> Unit
    ): String {
        val responseBody = service.streamGenerateContent(
            model = model,
            apiKey = apiKey,
            alt = "sse",
            request = request
        )

        val accumulated = StringBuilder()
        val rawFallbackBuffer = StringBuilder()
        var sawSseDataLine = false

        responseBody.use { body ->
            body.charStream().buffered(4096).useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    if (trimmed.startsWith("data:")) {
                        sawSseDataLine = true
                        val payload = trimmed.removePrefix("data:").trim()
                        if (payload.isEmpty() || payload == "[DONE]") continue

                        val parsedChunk = runCatching {
                            json.decodeFromString<GenerateContentResponse>(payload)
                        }.getOrNull()

                        if (parsedChunk != null) {
                            checkSafetyOrBlock(parsedChunk)
                            val chunkText = extractCandidateText(parsedChunk)
                            if (chunkText.isNotEmpty()) {
                                accumulated.append(chunkText)
                                onPartialText(accumulated.toString())
                            }
                        }
                    } else if (!sawSseDataLine && rawFallbackBuffer.length < 65_536) {
                        rawFallbackBuffer.append(trimmed)
                    }
                }
            }
        }

        if (accumulated.isNotEmpty()) {
            return accumulated.toString().trim()
        }

        // In case an intermediary returned a single JSON object or JSON array instead of SSE lines
        val fallbackRaw = rawFallbackBuffer.toString().trim()
        if (fallbackRaw.isNotEmpty()) {
            val singleParsed = runCatching {
                json.decodeFromString<GenerateContentResponse>(fallbackRaw)
            }.getOrNull()
            if (singleParsed != null) {
                checkSafetyOrBlock(singleParsed)
                val text = extractCandidateText(singleParsed).trim()
                if (text.isNotEmpty()) {
                    onPartialText(text)
                    return text
                }
            }

            val listParsed = runCatching {
                json.decodeFromString<List<GenerateContentResponse>>(fallbackRaw)
            }.getOrNull()
            if (!listParsed.isNullOrEmpty()) {
                val combined = StringBuilder()
                for (item in listParsed) {
                    checkSafetyOrBlock(item)
                    combined.append(extractCandidateText(item))
                }
                val result = combined.toString().trim()
                if (result.isNotEmpty()) {
                    onPartialText(result)
                    return result
                }
            }
        }

        return ""
    }

    private fun extractTextFromResponse(response: GenerateContentResponse?): String {
        if (response == null) return ""
        checkSafetyOrBlock(response)
        return extractCandidateText(response).trim()
    }

    private fun checkSafetyOrBlock(response: GenerateContentResponse) {
        val blockReason = response.promptFeedback?.blockReason
        if (!blockReason.isNullOrBlank()) {
            throw GeminiRemoteException.SafetyBlockedException("Pesan diblokir oleh filter keamanan ($blockReason).")
        }
        val finishReason = response.candidates?.firstOrNull()?.finishReason
        if (finishReason.equals("SAFETY", ignoreCase = true) ||
            finishReason.equals("PROHIBITED_CONTENT", ignoreCase = true) ||
            finishReason.equals("BLOCKLIST", ignoreCase = true)
        ) {
            throw GeminiRemoteException.SafetyBlockedException("Respons dibatasi oleh filter keamanan.")
        }
    }

    private fun extractCandidateText(response: GenerateContentResponse): String {
        val candidates = response.candidates ?: return ""
        for (candidate in candidates) {
            val parts = candidate.content?.parts ?: continue
            val text = parts.mapNotNull { it.text }.joinToString("")
            if (text.isNotEmpty()) {
                return text
            }
        }
        return ""
    }

    private fun isRetryableHttpStatus(statusCode: Int): Boolean {
        return statusCode == 408 || statusCode == 429 || statusCode in 500..599
    }

    private fun isApiKeyInvalidError(statusCode: Int, errorBody: String): Boolean {
        if (statusCode == 401 || statusCode == 403) return true
        if (statusCode == 400) {
            val lower = errorBody.lowercase()
            if (lower.contains("api_key_invalid") ||
                lower.contains("api key not valid") ||
                lower.contains("api key expired") ||
                lower.contains("invalid api key")
            ) {
                return true
            }
        }
        return false
    }

    fun extractHttpStatusCode(e: HttpException): Int {
        return try {
            e.response()?.raw()?.code ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun extractSafeErrorBody(e: HttpException, apiKey: String): String {
        return try {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            sanitizeSensitiveText(raw, apiKey)
        } catch (_: Exception) {
            ""
        }
    }

    fun sanitizeSensitiveText(input: String?, apiKey: String): String {
        if (input.isNullOrBlank()) return ""
        var result = input
        if (apiKey.isNotBlank()) {
            result = result.replace(apiKey, "[REDACTED_KEY]", ignoreCase = true)
        }
        result = result.replace(Regex("AIza[0-9A-Za-z_-]{20,}"), "[REDACTED_KEY]")
        result = result.replace(Regex("key=[^&\\s]+"), "key=[REDACTED]")
        return result
    }
}
