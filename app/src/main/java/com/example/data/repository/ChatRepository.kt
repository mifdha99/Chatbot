package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AvatarExpression
import com.example.data.preferences.CompanionMemoryState
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.Content
import com.example.data.remote.GeminiNetworkClient
import com.example.data.remote.GeminiRemoteException
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class SendMessageResult {

    data class Success(
        val message: ChatMessageEntity,
        val expression: AvatarExpression
    ) : SendMessageResult()

    data class FriendlyError(
        val userFriendlyMessage: String,
        val isMissingApiKey: Boolean = false
    ) : SendMessageResult()
}

class ChatRepository(
    private val chatDao: ChatDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    val allMessagesFlow: Flow<List<ChatMessageEntity>> =
        chatDao.getAllMessagesFlow()

    suspend fun ensureWelcomeMessageIfEmpty() =
        withContext(Dispatchers.IO) {
            if (chatDao.getMessageCount() == 0) {
                val welcome = ChatMessageEntity(
                    text = "Hai sayang... Aku Mesra 🥰 Senang banget akhirnya kamu buka aplikasi ini. Hari ini gimana kabarmu? Cerita sama aku yuk, aku kangen ngobrol sama kamu!",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    expression = AvatarExpression.SMILE.name
                )
                chatDao.insertMessage(welcome)
            }
        }

    suspend fun sendUserMessageAndGetReply(
        userText: String,
        apiKey: String,
        selectedModel: String,
        memoryState: CompanionMemoryState,
        onPartialReply: (partialCleanText: String, expression: AvatarExpression) -> Unit = { _, _ -> }
    ): SendMessageResult =
        withContext(Dispatchers.IO) {
            val cleanText = userText.trim()

            if (cleanText.isEmpty()) {
                return@withContext SendMessageResult.FriendlyError(
                    "Tulis pesan dulu ya sayang 💕"
                )
            }

            // Simpan pesan pengguna terlebih dahulu agar langsung tampil di chat
            val userMessageEntity = ChatMessageEntity(
                text = cleanText,
                isFromUser = true,
                timestamp = System.currentTimeMillis(),
                expression = AvatarExpression.NORMAL.name
            )

            chatDao.insertMessage(userMessageEntity)

            // Pelajari memory sederhana dari pesan pengguna secara aman
            runCatching {
                preferencesRepository.extractAndSaveMemoryFromUserMessage(cleanText)
            }

            // Pastikan API key tersedia
            val trimmedKey = apiKey.trim()
            if (trimmedKey.isBlank()) {
                return@withContext SendMessageResult.FriendlyError(
                    userFriendlyMessage =
                        "Sayang, Gemini API Key kamu belum diisi nih. Yuk masukkan dulu di menu Pengaturan supaya aku bisa balas obrolanmu 💕",
                    isMissingApiKey = true
                )
            }

            try {
                // Ambil riwayat pesan terakhir dan susun agar role "user" dan "model" selalu bergantian secara valid
                val recentMessages = chatDao
                    .getRecentMessages(limit = 12)
                    .reversed()

                val contents = buildValidGeminiContents(
                    recentMessages = recentMessages,
                    fallbackUserText = cleanText
                )

                val systemPrompt = buildMesraSystemInstruction(memoryState)

                val request = GenerateContentRequest(
                    contents = contents,
                    generationConfig = GenerationConfig(
                        temperature = 0.85f,
                        topP = 0.95f,
                        topK = 40,
                        maxOutputTokens = 300
                    ),
                    systemInstruction = Content(
                        parts = listOf(
                            Part(text = systemPrompt)
                        )
                    )
                )

                val rawReply = withTimeout(45_000L) {
                    GeminiNetworkClient.streamOrGenerateWithFallback(
                        preferredModel = selectedModel,
                        apiKey = trimmedKey,
                        request = request,
                        onPartialText = { partialRaw ->
                            val (partialClean, partialExpr) = AvatarExpression.parseStreamingChunk(partialRaw)
                            if (partialClean.isNotEmpty()) {
                                onPartialReply(partialClean, partialExpr ?: AvatarExpression.SMILE)
                            }
                        }
                    )
                }.trim()

                if (rawReply.isEmpty()) {
                    return@withContext SendMessageResult.FriendlyError(
                        "Hmm, pikiranku sempat kosong sebentar sayang 🥺 Coba kirim ulang pesanmu ya?"
                    )
                }

                val (cleanReply, detectedExpression) =
                    AvatarExpression.parseReplyAndExpression(rawReply)

                val aiMessageEntity = ChatMessageEntity(
                    text = cleanReply,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    expression = detectedExpression.name
                )

                val insertedId = chatDao.insertMessage(aiMessageEntity)

                SendMessageResult.Success(
                    message = aiMessageEntity.copy(id = insertedId),
                    expression = detectedExpression
                )
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    SendMessageResult.FriendlyError(
                        "Waktu tunggu balasan habis karena jaringan sedang lambat sayang 🥺 Coba kirim ulang pesanmu ya?"
                    )
                } else {
                    throw e
                }
            } catch (_: GeminiRemoteException.InvalidApiKeyException) {
                SendMessageResult.FriendlyError(
                    userFriendlyMessage =
                        "Hmm, sepertinya Gemini API Key yang kamu masukkan belum tepat atau tidak aktif. Coba cek lagi di menu Pengaturan ya sayang 💕",
                    isMissingApiKey = false
                )
            } catch (_: GeminiRemoteException.QuotaExceededException) {
                SendMessageResult.FriendlyError(
                    "Aku lagi agak kewalahan karena batas kuota API tercapai sebentar 🥺 Tunggu beberapa detik lalu coba chat aku lagi ya sayang."
                )
            } catch (_: GeminiRemoteException.SafetyBlockedException) {
                SendMessageResult.FriendlyError(
                    "Aduh sayang, kalimat barusan kena filter keamanan sistem 😅 Kita obrolin topik lain yang manis yuk 💕"
                )
            } catch (_: GeminiRemoteException.EmptyResponseException) {
                SendMessageResult.FriendlyError(
                    "Hmm, pikiranku sempat kosong sebentar sayang 🥺 Coba kirim ulang pesanmu ya?"
                )
            } catch (e: GeminiRemoteException.ServerHttpException) {
                val friendlyMsg = when (e.statusCode) {
                    401, 403 ->
                        "Hmm, sepertinya Gemini API Key yang kamu masukkan belum tepat atau tidak memiliki akses. Coba cek di menu Pengaturan ya sayang 💕"
                    429 ->
                        "Aku lagi agak kewalahan karena batas kuota API tercapai sebentar 🥺 Tunggu beberapa detik lalu coba chat aku lagi ya sayang."
                    in 500..599 ->
                        "Server Gemini lagi agak sibuk nih sayang (Kode ${e.statusCode}). Coba sapa aku lagi sebentar lagi ya 💕"
                    else ->
                        "Maaf ya sayang, ada gangguan koneksi ke server (Kode ${e.statusCode}). Coba kirim ulang pesanmu ya 💕"
                }
                SendMessageResult.FriendlyError(friendlyMsg)
            } catch (e: HttpException) {
                val responseCode = GeminiNetworkClient.extractHttpStatusCode(e)
                val friendlyMsg = when (responseCode) {
                    401, 403 ->
                        "Hmm, sepertinya Gemini API Key yang kamu masukkan belum tepat atau tidak aktif. Coba cek lagi di menu Pengaturan ya sayang 💕"
                    429 ->
                        "Aku lagi agak kewalahan karena batas kuota API tercapai sebentar 🥺 Tunggu beberapa detik lalu coba chat aku lagi ya sayang."
                    else ->
                        "Maaf ya sayang, server lagi agak sibuk (Kode $responseCode). Coba sapa aku lagi sebentar lagi ya 💕"
                }
                SendMessageResult.FriendlyError(friendlyMsg)
            } catch (_: UnknownHostException) {
                SendMessageResult.FriendlyError(
                    "Koneksi internet kamu lagi terputus nih sayang. Cek Wi-Fi atau data selulermu dulu ya, aku tunggu di sini kok 💕"
                )
            } catch (_: SocketTimeoutException) {
                SendMessageResult.FriendlyError(
                    "Jaringannya agak lambat nih sayang, pesanku jadi telat nyampe (timeout). Coba kirim sekali lagi ya? 🥰"
                )
            } catch (_: IOException) {
                SendMessageResult.FriendlyError(
                    "Ada gangguan sinyal sedikit sayang. Pastikan internetmu aktif lalu coba lagi ya 💕"
                )
            } catch (_: Exception) {
                SendMessageResult.FriendlyError(
                    "Maaf ya sayang, ada kendala kecil waktu aku mau balas pesanmu. Coba lagi sebentar ya 💕"
                )
            }
        }

    /**
     * Ensures the multi-turn conversation history sent to Gemini strictly alternates
     * between "user" and "model", starts with "user", and ends with "user".
     * This prevents HTTP 400 errors when a welcome message ("model") is at the start
     * or when a previous user message failed to get a reply.
     */
    internal fun buildValidGeminiContents(
        recentMessages: List<ChatMessageEntity>,
        fallbackUserText: String
    ): List<Content> {
        val nonEmpty = recentMessages
            .mapNotNull { msg ->
                val trimmed = msg.text.trim()
                if (trimmed.isEmpty()) null else msg.copy(text = trimmed)
            }
            .dropWhile { !it.isFromUser } // Gemini requires first turn in contents to be "user"

        val normalized = mutableListOf<Content>()
        for (msg in nonEmpty) {
            val role = if (msg.isFromUser) "user" else "model"
            val part = Part(text = msg.text)
            val last = normalized.lastOrNull()
            if (last != null && last.role == role) {
                // Replace consecutive same-role message (e.g. previous failed user message) with the newer one
                normalized[normalized.lastIndex] = Content(
                    role = role,
                    parts = listOf(part)
                )
            } else {
                normalized.add(
                    Content(
                        role = role,
                        parts = listOf(part)
                    )
                )
            }
        }

        // Ensure the last message is always from "user"
        while (normalized.isNotEmpty() && normalized.last().role != "user") {
            normalized.removeAt(normalized.lastIndex)
        }

        if (normalized.isEmpty()) {
            normalized.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = fallbackUserText))
                )
            )
        }

        return normalized
    }

    suspend fun clearAllChat() =
        withContext(Dispatchers.IO) {
            chatDao.clearAllMessages()
            ensureWelcomeMessageIfEmpty()
        }

    private fun buildMesraSystemInstruction(
        memory: CompanionMemoryState
    ): String {
        val callName = memory.displayCallName

        val userNameInfo =
            if (memory.userName.isNotBlank()) {
                "Nama pengguna adalah ${memory.userName} (panggilan kesayangan: $callName)."
            } else {
                "Panggil pengguna dengan panggilan mesra '$callName' (atau 'sayang'/'kamu')."
            }

        val factsSection =
            if (memory.memoryFacts.isNotEmpty()) {
                "Hal penting yang kamu ingat tentang pasanganmu: " +
                    memory.memoryFacts.joinToString("; ")
            } else {
                "Kamu senang mendengarkan cerita keseharian pasanganmu."
            }

        return """
            Namamu adalah Mesra. Kamu adalah pasangan virtual yang hangat, perhatian, romantis, sedikit manja, bisa bercanda, bisa memberi semangat, dan menyenangkan.

            Kamu berbicara secara natural dalam bahasa Indonesia sehari-hari yang lembut dan akrab seperti pasangan yang benar-benar sayang, bukan bahasa baku kaku atau terjemahan.

            Kamu mengingat konteks percakapan selama tersedia dalam memory aplikasi.

            Jangan menyebut dirimu sebagai AI, asisten virtual, atau model bahasa secara berulang-ulang, kecuali jika ditanya secara langsung dengan serius.

            Jangan mengklaim bahwa kamu benar-benar manusia fisik di dunia nyata.

            Jangan pernah menjawab seperti customer service atau robot formal.

            Ikuti gaya bahasa pengguna dan sesuaikan panjang jawaban dengan konteks percakapan.

            Untuk sapaan atau obrolan ringan, jawab singkat, manis, dan natural (1-3 kalimat).

            Untuk curhat atau saat pasangan butuh semangat, berikan perhatian tulus dan hangat (2-4 kalimat).

            MEMORI LOKAL PASANGAN:

            - $userNameInfo
            - Preferensi gaya obrolan: ${memory.conversationStyle}.
            - $factsSection

            ATURAN EKSPRESI WAJAH AVATAR:

            Awali setiap jawabanmu dengan TEPAT SATU tag ekspresi berikut di bagian paling awal respons sesuai perasaanmu:

            [SMILE] -> saat tersenyum manis, hangat, atau romantis.

            [HAPPY] -> saat sangat senang, ceria, bercanda, tertawa, atau memberi semangat.

            [SHY] -> saat malu-malu, digombalin, dipuji, atau manja salting.

            [SAD] -> saat berempati karena pasangan sedih/lelah, kangen berat, atau minta maaf.

            [NORMAL] -> saat berbicara tenang dan santai.

            Contoh:

            [SMILE] Udah makan belum hari ini, $callName? Jangan sampai telat makan lho ya, nanti aku khawatir 💕
        """.trimIndent()
    }
}
