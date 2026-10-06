package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MesraDatabase
import com.example.data.model.AvatarExpression
import com.example.data.preferences.AppSettingsState
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.SendMessageResult
import com.example.voice.TtsLipSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MesraDatabase.getDatabase(application)
    private val preferencesRepository = UserPreferencesRepository(application)
    private val chatRepository = ChatRepository(database.chatDao(), preferencesRepository)
    private val ttsManager = TtsLipSyncManager(application)

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepository.allMessagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<AppSettingsState> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsState()
        )

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val mouthOpenness: StateFlow<Float> = ttsManager.mouthOpenness

    private val _currentExpression = MutableStateFlow(AvatarExpression.SMILE)
    val currentExpression: StateFlow<AvatarExpression> = _currentExpression.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friendlyBannerMessage = MutableStateFlow<String?>(null)
    val friendlyBannerMessage: StateFlow<String?> = _friendlyBannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.ensureWelcomeMessageIfEmpty()
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _friendlyBannerMessage.value = null
            _isLoading.value = true
            ttsManager.stopSpeaking()

            val currentSettings = preferencesRepository.settingsFlow.first()
            val result = chatRepository.sendUserMessageAndGetReply(
                userText = trimmed,
                apiKey = currentSettings.effectiveApiKey,
                selectedModel = currentSettings.selectedModel,
                memoryState = currentSettings.memory
            )

            _isLoading.value = false

            when (result) {
                is SendMessageResult.Success -> {
                    _currentExpression.value = result.expression
                    // Sequence: 1. Display reply -> 2. Avatar talking -> 3. TTS id-ID speaks -> 4. Lip-sync -> 5. Back to idle
                    ttsManager.speak(
                        rawText = result.message.text,
                        isVoiceEnabled = currentSettings.isTtsEnabled
                    )
                }

                is SendMessageResult.FriendlyError -> {
                    _currentExpression.value = AvatarExpression.SAD
                    _friendlyBannerMessage.value = result.userFriendlyMessage
                }
            }
        }
    }

    fun setExpression(expression: AvatarExpression) {
        _currentExpression.value = expression
    }

    fun toggleTtsEnabled() {
        viewModelScope.launch {
            val current = settings.value.isTtsEnabled
            val next = !current
            preferencesRepository.setTtsEnabled(next)
            if (!next) {
                ttsManager.stopSpeaking()
            } else {
                // Speak latest AI message if user turns voice back on
                val latestAiMsg = messages.value.lastOrNull { !it.isFromUser }?.text.orEmpty()
                if (latestAiMsg.isNotBlank()) {
                    ttsManager.speak(latestAiMsg, isVoiceEnabled = true)
                }
            }
        }
    }

    fun speakMessage(text: String, expression: AvatarExpression? = null) {
        if (expression != null) {
            _currentExpression.value = expression
        }
        ttsManager.speak(text, isVoiceEnabled = true)
    }

    fun replayLastAiVoice() {
        val latestAiMessage = messages.value.lastOrNull { !it.isFromUser }
        if (latestAiMessage != null) {
            _currentExpression.value = latestAiMessage.avatarExpression
            ttsManager.replayLastSpeech(fallbackText = latestAiMessage.text)
        }
    }

    fun saveSettings(apiKey: String, model: String, ttsEnabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveApiKey(apiKey)
            preferencesRepository.saveSelectedModel(model)
            preferencesRepository.setTtsEnabled(ttsEnabled)
            if (!ttsEnabled) {
                ttsManager.stopSpeaking()
            }
            if (apiKey.isNotBlank()) {
                _friendlyBannerMessage.value = "Yeay! Gemini API Key berhasil disimpan. Sekarang kita bisa ngobrol sepuasnya sayang 🥰"
                _currentExpression.value = AvatarExpression.HAPPY
            }
        }
    }

    fun saveManualMemory(
        userName: String,
        nickname: String,
        style: String,
        facts: List<String>
    ) {
        viewModelScope.launch {
            preferencesRepository.updateMemoryManual(userName, nickname, style, facts)
            _friendlyBannerMessage.value = "Memory pasangan berhasil diperbarui 💖"
        }
    }

    fun resetMemory() {
        viewModelScope.launch {
            preferencesRepository.resetMemory()
            _currentExpression.value = AvatarExpression.NORMAL
            _friendlyBannerMessage.value = "Memory pasangan telah di-reset ke pengaturan awal."
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            ttsManager.stopSpeaking()
            chatRepository.clearAllChat()
            _currentExpression.value = AvatarExpression.SMILE
            _friendlyBannerMessage.value = null
        }
    }

    fun dismissBanner() {
        _friendlyBannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
