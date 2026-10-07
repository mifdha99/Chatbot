package com.example.data.preferences

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AvatarExpression
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mesra_user_prefs"
)

data class CompanionMemoryState(
    val userName: String = "",
    val userNickname: String = "Sayang",
    val conversationStyle: String = "Hangat, romantis, sedikit manja, & natural",
    val memoryFacts: List<String> = emptyList(),
    val lastTopicSummary: String = ""
) {
    val displayCallName: String
        get() = userName.ifBlank {
            userNickname.ifBlank { "Sayang" }
        }
}

data class AppSettingsState(
    val customApiKey: String = "",
    val selectedModel: String = DEFAULT_GEMINI_MODEL,
    val isTtsEnabled: Boolean = true,
    val memory: CompanionMemoryState = CompanionMemoryState()
) {
    val effectiveApiKey: String
        get() {
            val userKey = customApiKey.trim()
            if (userKey.isNotEmpty()) return userKey
            return resolveBuildConfigApiKey()
        }

    val isApiKeyConfigured: Boolean
        get() = effectiveApiKey.isNotEmpty()

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"

        private val cachedBuildConfigKey: String by lazy {
            try {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.getDeclaredField("GEMINI_API_KEY")
                field.isAccessible = true
                val raw = (field.get(null) as? String)?.trim().orEmpty()
                if (raw.isNotEmpty() &&
                    raw != "MY_GEMINI_API_KEY" &&
                    raw != "null" &&
                    !raw.startsWith("YOUR_")
                ) {
                    raw
                } else {
                    ""
                }
            } catch (_: Throwable) {
                ""
            }
        }

        fun resolveBuildConfigApiKey(): String = cachedBuildConfigKey
    }
}

class UserPreferencesRepository(
    context: Context
) {
    private val context: Context = context.applicationContext

    private object Keys {
        val ENCODED_API_KEY = stringPreferencesKey("encoded_gemini_api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_gemini_model")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val USER_NAME = stringPreferencesKey("memory_user_name")
        val USER_NICKNAME = stringPreferencesKey("memory_user_nickname")
        val CONVERSATION_STYLE = stringPreferencesKey("memory_conversation_style")
        val MEMORY_FACTS = stringPreferencesKey("memory_facts_delimited")
        val LAST_TOPIC = stringPreferencesKey("memory_last_topic")
    }

    val settingsFlow: Flow<AppSettingsState> =
        context.dataStore.data.map { prefs ->

            val encodedKey = prefs[Keys.ENCODED_API_KEY].orEmpty()
            val decodedKey = decodeKey(encodedKey)

            val rawFacts = prefs[Keys.MEMORY_FACTS].orEmpty()

            val factsList = rawFacts
                .split("||")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            AppSettingsState(
                customApiKey = decodedKey,
                selectedModel = prefs[Keys.SELECTED_MODEL]
                    ?: AppSettingsState.DEFAULT_GEMINI_MODEL,
                isTtsEnabled = prefs[Keys.TTS_ENABLED] ?: true,
                memory = CompanionMemoryState(
                    userName = prefs[Keys.USER_NAME].orEmpty(),
                    userNickname = prefs[Keys.USER_NICKNAME] ?: "Sayang",
                    conversationStyle = prefs[Keys.CONVERSATION_STYLE]
                        ?: "Hangat, romantis, sedikit manja, & natural",
                    memoryFacts = factsList,
                    lastTopicSummary = prefs[Keys.LAST_TOPIC].orEmpty()
                )
            )
        }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENCODED_API_KEY] = encodeKey(apiKey.trim())
        }
    }

    suspend fun saveSelectedModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_MODEL] =
                model.trim().ifEmpty {
                    AppSettingsState.DEFAULT_GEMINI_MODEL
                }
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TTS_ENABLED] = enabled
        }
    }

    suspend fun updateMemoryManual(
        userName: String,
        userNickname: String,
        conversationStyle: String,
        facts: List<String>
    ) {
        context.dataStore.edit { prefs ->

            prefs[Keys.USER_NAME] = userName.trim()

            prefs[Keys.USER_NICKNAME] =
                userNickname.trim().ifEmpty { "Sayang" }

            prefs[Keys.CONVERSATION_STYLE] =
                conversationStyle.trim().ifEmpty {
                    "Hangat, romantis, sedikit manja, & natural"
                }

            prefs[Keys.MEMORY_FACTS] =
                facts
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .takeLast(8)
                    .joinToString("||")
        }
    }

    suspend fun extractAndSaveMemoryFromUserMessage(
        userMessage: String
    ) {
        val text = userMessage.trim()

        if (text.length < 3) return

        context.dataStore.edit { prefs ->

            // 1. Deteksi nama pengguna
            val nameRegex = Regex(
                """(?:namaku|nama\s+aku|nama\s+saya|kenalkan\s+aku)\s+(?:adalah\s+)?([A-Za-z]{2,20})""",
                RegexOption.IGNORE_CASE
            )

            nameRegex
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { detectedName ->

                    val cleanName =
                        detectedName.replaceFirstChar {
                            it.uppercase()
                        }

                    prefs[Keys.USER_NAME] = cleanName
                }

            // 2. Deteksi panggilan kesukaan
            val nickRegex = Regex(
                """panggil\s+aku\s+([A-Za-z\s]{2,22})""",
                RegexOption.IGNORE_CASE
            )

            nickRegex
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { rawNick ->

                    val cleanNick =
                        rawNick
                            .trim()
                            .split(Regex("\\s+"))
                            .take(2)
                            .joinToString(" ")

                    if (cleanNick.isNotEmpty()) {
                        prefs[Keys.USER_NICKNAME] = cleanNick
                    }
                }

            // 3. Deteksi gaya percakapan
            val lower = text.lowercase()

            when {
                lower.contains("jangan terlalu panjang") ||
                    lower.contains("singkat aja") -> {

                    prefs[Keys.CONVERSATION_STYLE] =
                        "Singkat, padat, tetap romantis & hangat"
                }

                lower.contains("lebih manja") ||
                    lower.contains("yang manja") -> {

                    prefs[Keys.CONVERSATION_STYLE] =
                        "Sangat manja, menggemaskan, & penuh perhatian"
                }

                lower.contains("santai") ||
                    lower.contains("bercanda") ||
                    lower.contains("lucu") -> {

                    prefs[Keys.CONVERSATION_STYLE] =
                        "Santai, suka bercanda, hangat, & akrab"
                }
            }

            // 4. Simpan fakta sederhana tentang pengguna
            val existingFacts =
                prefs[Keys.MEMORY_FACTS]
                    .orEmpty()
                    .split("||")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()

            val factPatterns = listOf(

                Regex(
                    """(aku\s+(?:suka|senang|hobi|benci|alergi|kerja|kuliah|tinggal)\s+[^.,!?]{3,45})""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """(hobiku\s+[^.,!?]{3,40})""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """(makanan\s+favoritku\s+[^.,!?]{3,40})""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """(hari\s+ini\s+aku\s+[^.,!?]{3,45})""",
                    RegexOption.IGNORE_CASE
                )
            )

            for (pattern in factPatterns) {

                pattern
                    .find(text)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let { matchedFact ->

                        val normalized =
                            matchedFact
                                .trim()
                                .take(60)

                        if (
                            existingFacts.none {
                                it.equals(
                                    normalized,
                                    ignoreCase = true
                                )
                            }
                        ) {
                            existingFacts.add(normalized)
                        }
                    }
            }

            // Maksimal 6 fakta
            while (existingFacts.size > 6) {
                existingFacts.removeAt(0)
            }

            prefs[Keys.MEMORY_FACTS] =
                existingFacts.joinToString("||")

            // Simpan topik terakhir
            prefs[Keys.LAST_TOPIC] =
                text.take(80)
        }
    }

    suspend fun resetMemory() {
        context.dataStore.edit { prefs ->

            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_NICKNAME)
            prefs.remove(Keys.CONVERSATION_STYLE)
            prefs.remove(Keys.MEMORY_FACTS)
            prefs.remove(Keys.LAST_TOPIC)
        }
    }

    private fun encodeKey(raw: String): String {

        if (raw.isEmpty()) return ""

        return Base64.encodeToString(
            raw.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
    }

    private fun decodeKey(encoded: String): String {

        if (encoded.isEmpty()) return ""

        return try {

            String(
                Base64.decode(
                    encoded,
                    Base64.NO_WRAP
                ),
                Charsets.UTF_8
            )

        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
