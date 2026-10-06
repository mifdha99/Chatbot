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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
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
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val idLocale = Locale.forLanguageTag("id-ID")
            val result = engine.setLanguage(idLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to legacy Indonesian tag "in-ID" or device default
                val fallbackLocale = Locale("in", "ID")
                val fallbackResult = engine.setLanguage(fallbackLocale)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.getDefault())
                }
            }
            // Warm, natural female-like pitch and comfortable conversational cadence
            engine.setPitch(1.08f)
            engine.setSpeechRate(0.98f)

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

        // Estimate realistic duration in ms (~72ms per character in Indonesian, bounded 1.8s..18s)
        val estimatedDurationMs = (cleanSpeechText.length * 72L).coerceIn(1800L, 18000L)

        startLipSyncAnimation(cleanSpeechText, estimatedDurationMs)

        val engine = tts
        if (isTtsReady && engine != null) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val speakResult = engine.speak(
                cleanSpeechText,
                TextToSpeech.QUEUE_FLUSH,
                params,
                utteranceId
            )
            if (speakResult == TextToSpeech.ERROR) {
                // Keep visual lip-sync running for estimated duration even if emulator lacks audio output
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
        try {
            tts?.stop()
        } catch (_: Exception) {
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

                // Sample character position along the utterance to pause naturally on punctuation
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
                    val SecondaryWave = abs(sin(step * 1.15f + 0.8f)) * 0.35f
                    val openness = ((wave * 0.7f + SecondaryWave) * vowelWeight).coerceIn(0.12f, 1.0f)
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
     * Strips emojis, markdown symbols, and bracket tags so Android TTS reads smooth Indonesian sentences.
     */
    private fun sanitizeForIndonesianTts(input: String): String {
        return input
            .replace(Regex("""\[[^\]]*]"""), "") // remove [SMILE] etc.
            .replace(Regex("""[*_~`#>]"""), "") // remove markdown symbols
            .replace(
                Regex("""[\p{So}\p{Cn}\uFE0F\u200D]+"""),
                " "
            ) // remove emojis & symbols
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun shutdown() {
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        scope.cancel()
    }
}
