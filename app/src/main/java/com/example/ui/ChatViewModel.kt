package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MesraDatabase
import com.example.data.model.AvatarExpression
import com.example.data.preferences.AppSettingsState
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.SendMessageResult
import com.example.voice.TtsLipSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MesraDatabase.getDatabase(application)
    private val preferencesRepository = UserPreferencesRepository.getInstance(application)
    private val chatRepository = ChatRepository(database.chatDao(), preferencesRepository)
    private val ttsManager = TtsLipSyncManager(application)

    private val isSendingLock = AtomicBoolean(false)

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepository.allMessagesFlow
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<AppSettingsState> = preferencesRepository.settingsFlow
        .catch { emit(AppSettingsState()) }
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

    private val _streamingReplyText = MutableStateFlow("")
    val streamingReplyText: StateFlow<String> = _streamingReplyText.asStateFlow()

    private val _friendlyBannerMessage = MutableStateFlow<String?>(null)
    val friendlyBannerMessage: StateFlow<String?> = _friendlyBannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                chatRepository.ensureWelcomeMessageIfEmpty()
            }
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        // Atomic double-submit guard before coroutine launch
        if (!isSendingLock.compareAndSet(false, true)) return
        _isLoading.value = true
        _streamingReplyText.value = ""
        _friendlyBannerMessage.value = null

        viewModelScope.launch {
            try {
                ttsManager.stopSpeaking()

                val currentSettings = runCatching {
                    preferencesRepository.settingsFlow.first()
                }.getOrDefault(AppSettingsState())

                val result = try {
                    chatRepository.sendUserMessageAndGetReply(
                        userText = trimmed,
                        apiKey = currentSettings.effectiveApiKey,
                        selectedModel = currentSettings.selectedModel,
                        memoryState = currentSettings.memory,
                        onPartialReply = { partialText, partialExpression ->
                            _streamingReplyText.value = partialText
                            _currentExpression.value = partialExpression
                        }
                    )
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    SendMessageResult.FriendlyError(
                        "Maaf ya sayang, ada kendala kecil waktu memproses pesanmu. Coba lagi sebentar ya 💕"
                    )
                }

                when (result) {
                    is SendMessageResult.Success -> {
                        _currentExpression.value = result.expression
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
            } finally {
                _streamingReplyText.value = ""
                _isLoading.value = false
                isSendingLock.set(false)
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
                _friendlyBannerMessage.value =
                    "Yeay! Gemini API Key berhasil disimpan. Sekarang kita bisa ngobrol sepuasnya sayang 🥰"
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
            runCatching { chatRepository.clearAllChat() }
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

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(application) as T
                }
            }
    }
}
