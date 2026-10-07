package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

class TtsLipSyncManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lipSyncJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _mouthOpenness = MutableStateFlow(0f)
    val mouthOpenness: StateFlow<Float> = _mouthOpenness.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(appContext, this)
        }.onFailure {
            isTtsReady = false
        }
    }

    override fun onInit(status: Int) {
        runCatching {
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return
                val idLocale = Locale.forLanguageTag("id-ID")
                val result = runCatching { engine.setLanguage(idLocale) }
                    .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallbackLocale = Locale.forLanguageTag("in-ID")
                    val fallbackResult = runCatching { engine.setLanguage(fallbackLocale) }
                        .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                    if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        runCatching { engine.setLanguage(Locale.getDefault()) }
                    }
                }

                runCatching { engine.setPitch(1.08f) }
                runCatching { engine.setSpeechRate(0.98f) }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        stopLipSyncInternal()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        stopLipSyncInternal()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        stopLipSyncInternal()
                    }
                })
                isTtsReady = true
            }
        }.onFailure {
            isTtsReady = false
        }
    }

    /**
     * Speaks the given Indonesian response and drives natural syllable-cadence lip-sync
     * for the avatar until the speech finishes.
     */
    fun speak(rawText: String, isVoiceEnabled: Boolean = true) {
        val cleanSpeechText = sanitizeForIndonesianTts(rawText)
        if (cleanSpeechText.isBlank()) return

        _lastSpokenText.value = cleanSpeechText
        if (!isVoiceEnabled) {
            stopSpeaking()
            return
        }

        stopLipSyncInternal()
        val utteranceId = "mesra_tts_${UUID.randomUUID()}"
        val estimatedDurationMs = (cleanSpeechText.length * 72L).coerceIn(1800L, 18000L)

        startLipSyncAnimation(cleanSpeechText, estimatedDurationMs)

        val engine = tts
        if (isTtsReady && engine != null) {
            runCatching {
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                engine.speak(
                    cleanSpeechText,
                    TextToSpeech.QUEUE_FLUSH,
                    params,
                    utteranceId
                )
            }
        }
    }

    fun replayLastSpeech(fallbackText: String = "") {
        val textToSpeak = _lastSpokenText.value.ifBlank { fallbackText }
        if (textToSpeak.isNotBlank()) {
            speak(textToSpeak, isVoiceEnabled = true)
        }
    }

    fun stopSpeaking() {
        runCatching {
            tts?.stop()
        }
        stopLipSyncInternal()
    }

    private fun startLipSyncAnimation(speechText: String, maxDurationMs: Long) {
        lipSyncJob?.cancel()
        _isSpeaking.value = true

        lipSyncJob = scope.launch {
            val startTime = System.currentTimeMillis()
            var step = 0
            val chars = speechText.toCharArray()

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= maxDurationMs) {
                    break
                }

                val progress = (elapsed.toFloat() / maxDurationMs.toFloat()).coerceIn(0f, 0.99f)
                val charIndex = (progress * chars.size).toInt().coerceIn(0, (chars.size - 1).coerceAtLeast(0))
                val currentChar = chars.getOrNull(charIndex) ?: 'a'

                val isPunctuationPause = currentChar == ',' || currentChar == '.' ||
                    currentChar == '?' || currentChar == '!' || currentChar == '…'

                if (isPunctuationPause) {
                    _mouthOpenness.value = 0.08f
                    delay(95L)
                } else {
                    val vowelWeight = when (currentChar.lowercaseChar()) {
                        'a', 'o' -> 0.95f
                        'e', 'i', 'u' -> 0.72f
                        ' ' -> 0.20f
                        else -> 0.55f
                    }
                    val wave = abs(sin(step * 0.65f))
                    val secondaryWave = abs(sin(step * 1.15f + 0.8f)) * 0.35f
                    val openness = ((wave * 0.7f + secondaryWave) * vowelWeight).coerceIn(0.12f, 1.0f)
                    _mouthOpenness.value = openness
                    delay(55L)
                }
                step++
            }

            stopLipSyncInternal()
        }
    }

    private fun stopLipSyncInternal() {
        lipSyncJob?.cancel()
        lipSyncJob = null
        _mouthOpenness.value = 0f
        _isSpeaking.value = false
    }

    /**
     * Strips bracket tags, markdown symbols, and emojis using character inspection (100% ICU-safe on all Android versions).
     */
    private fun sanitizeForIndonesianTts(input: String): String {
        if (input.isBlank()) return ""
        val withoutTags = input.replace(Regex("\\[[^\\]]*\\]"), " ")
        val sb = StringBuilder(withoutTags.length)
        for (ch in withoutTags) {
            val type = Character.getType(ch)
            val isEmojiOrSymbol = type == Character.SURROGATE.toInt() ||
                type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.NON_SPACING_MARK.toInt() ||
                ch == '*' || ch == '_' || ch == '~' || ch == '`' || ch == '#' || ch == '>'
            if (isEmojiOrSymbol) {
                sb.append(' ')
            } else {
                sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    fun shutdown() {
        stopSpeaking()
        runCatching {
            tts?.shutdown()
        }
        runCatching {
            scope.cancel()
        }
    }
}
