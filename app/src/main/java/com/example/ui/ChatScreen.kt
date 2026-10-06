package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatMessageEntity
import com.example.ui.components.ApiKeySettingsDialog
import com.example.ui.components.ClearChatConfirmDialog
import com.example.ui.components.CompanionMemoryDialog
import com.example.ui.components.LivingPartnerAvatarHeader
import com.example.ui.components.VoicePromptDialog
import com.example.ui.theme.AiBubbleBg
import com.example.ui.theme.AiBubbleBorder
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.MesraBorderSubtle
import com.example.ui.theme.MesraCardSurface
import com.example.ui.theme.MesraDarkBg
import com.example.ui.theme.MesraDarkSurface
import com.example.ui.theme.MesraElevatedSurface
import com.example.ui.theme.MesraMagentaAccent
import com.example.ui.theme.MesraPinkPrimary
import com.example.ui.theme.MesraSoftPink
import com.example.ui.theme.TextMutedMauve
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondarySoft
import com.example.ui.theme.UserBubbleEnd
import com.example.ui.theme.UserBubbleStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val mouthOpenness by viewModel.mouthOpenness.collectAsStateWithLifecycle()
    val currentExpression by viewModel.currentExpression.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.friendlyBannerMessage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // Android SpeechRecognizer state for Indonesian Voice Input
    var isVoiceListening by remember { mutableStateOf(false) }
    var partialSpokenText by remember { mutableStateOf("") }
    var voiceErrorText by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isVoiceListening = true
                voiceErrorText = null
            }

            override fun onBeginningOfSpeech() {
                isVoiceListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isVoiceListening = false
            }

            override fun onError(error: Int) {
                isVoiceListening = false
                voiceErrorText = "Suara belum terdengar jelas. Kamu juga bisa pilih kalimat cepat di bawah ya sayang 💕"
            }

            override fun onResults(results: Bundle?) {
                isVoiceListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim().orEmpty()
                if (recognized.isNotEmpty()) {
                    partialSpokenText = recognized
                    showVoiceDialog = false
                    viewModel.sendMessage(recognized)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    partialSpokenText = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    fun startSpeechRecognition() {
        if (speechRecognizer == null) {
            voiceErrorText = "Fitur mikrofon tidak tersedia di perangkat/emulator ini. Pilih pesan suara cepat di bawah ya 💕"
            return
        }
        try {
            partialSpokenText = ""
            voiceErrorText = null
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer.startListening(intent)
            isVoiceListening = true
        } catch (_: Exception) {
            isVoiceListening = false
            voiceErrorText = "Tidak dapat memulai mikrofon. Silakan pilih pesan cepat di bawah ya sayang."
        }
    }

    fun stopSpeechRecognition() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        isVoiceListening = false
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showVoiceDialog = true
        if (isGranted) {
            startSpeechRecognition()
        } else {
            voiceErrorText = "Izin mikrofon belum diberikan. Kamu tetap bisa memilih kalimat suara cepat di bawah 💕"
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to latest message whenever messages list changes or typing indicator appears
    LaunchedEffect(messages.size, isLoading) {
        val totalItems = messages.size + (if (isLoading) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MesraDarkBg),
        containerColor = MesraDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MesraTopAppBar(
                userCallName = settings.memory.displayCallName,
                isApiKeyConfigured = settings.isApiKeyConfigured,
                onOpenMemory = { showMemoryDialog = true },
                onQuickResetMemory = { viewModel.resetMemory() },
                onClearChat = { showClearChatDialog = true },
                onOpenSettings = { showSettingsDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // 1. Living Companion Avatar at the Top of the Chat Screen
            LivingPartnerAvatarHeader(
                expression = currentExpression,
                isSpeaking = isSpeaking,
                isTyping = isLoading,
                mouthOpenness = mouthOpenness,
                isTtsEnabled = settings.isTtsEnabled,
                canReplayVoice = messages.any { !it.isFromUser },
                userCallName = settings.memory.displayCallName,
                onExpressionSelected = { viewModel.setExpression(it) },
                onToggleTts = { viewModel.toggleTtsEnabled() },
                onReplayVoice = { viewModel.replayLastAiVoice() }
            )

            // 2. Clear API Key Configuration Card if Gemini API Key is not yet set
            AnimatedVisibility(
                visible = !settings.isApiKeyConfigured,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ApiKeySetupBannerCard(
                    onOpenSettings = { showSettingsDialog = true }
                )
            }

            // 3. Friendly Notification / Error Banner (non-crashing, warm Indonesian message)
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                bannerMessage?.let { msg ->
                    FriendlyNoticeBanner(
                        message = msg,
                        onDismiss = { viewModel.dismissBanner() },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
            }

            // 4. Chat Messages Area (Auto-scrolling LazyColumn)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    ChatBubbleItem(
                        message = message,
                        onReplayMessageVoice = {
                            viewModel.speakMessage(message.text, message.avatarExpression)
                        }
                    )
                }

                if (isLoading) {
                    item(key = "typing_indicator") {
                        TypingIndicatorBubble()
                    }
                }
            }

            // 5. Quick Romantic Conversation Prompts
            QuickRomanticSuggestionsRow(
                onSuggestionClick = { suggestion ->
                    viewModel.sendMessage(suggestion)
                }
            )

            // 6. Bottom Chat Input Bar + Floating Voice Action Button
            ChatBottomInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                isLoading = isLoading,
                isVoiceListening = isVoiceListening,
                onSendClick = {
                    val toSend = inputText.trim()
                    if (toSend.isNotEmpty()) {
                        inputText = ""
                        focusManager.clearFocus()
                        viewModel.sendMessage(toSend)
                    }
                },
                onVoiceButtonClick = {
                    val hasAudioPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasAudioPermission) {
                        showVoiceDialog = true
                        startSpeechRecognition()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
        }
    }

    // Modals & Dialogs
    if (showSettingsDialog) {
        ApiKeySettingsDialog(
            currentApiKey = settings.customApiKey,
            isUsingBuildConfigFallback = settings.customApiKey.isBlank() && settings.isApiKeyConfigured,
            selectedModel = settings.selectedModel,
            isTtsEnabled = settings.isTtsEnabled,
            onSaveSettings = { key, model, tts ->
                viewModel.saveSettings(key, model, tts)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showMemoryDialog) {
        CompanionMemoryDialog(
            memory = settings.memory,
            onSaveMemory = { name, nick, style, facts ->
                viewModel.saveManualMemory(name, nick, style, facts)
            },
            onResetMemory = {
                viewModel.resetMemory()
            },
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showClearChatDialog) {
        ClearChatConfirmDialog(
            onConfirmClear = { viewModel.clearChatHistory() },
            onDismiss = { showClearChatDialog = false }
        )
    }

    if (showVoiceDialog) {
        VoicePromptDialog(
            isListening = isVoiceListening,
            spokenPartialText = partialSpokenText,
            voiceErrorText = voiceErrorText,
            onStartListening = { startSpeechRecognition() },
            onStopListening = { stopSpeechRecognition() },
            onSendVoicePhrase = { phrase ->
                viewModel.sendMessage(phrase)
            },
            onDismiss = {
                stopSpeechRecognition()
                showVoiceDialog = false
            }
        )
    }
}

@Composable
private fun MesraTopAppBar(
    userCallName: String,
    isApiKeyConfigured: Boolean,
    onOpenMemory: () -> Unit,
    onQuickResetMemory: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = MesraDarkSurface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MesraPinkPrimary, MesraMagentaAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MesraAI",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MesraPinkPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Sayang: $userCallName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MesraSoftPink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Pasangan Virtual Romantis & Hangat",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySoft
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Memory Dialog Button
                IconButton(
                    onClick = onOpenMemory,
                    modifier = Modifier.testTag("memory_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = "Memory Pasangan",
                        tint = MesraSoftPink
                    )
                }

                // Direct Reset Memory Button
                IconButton(
                    onClick = onQuickResetMemory,
                    modifier = Modifier.testTag("top_reset_memory_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset Memory",
                        tint = TextSecondarySoft
                    )
                }

                // Clear Chat Button
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Hapus Chat",
                        tint = TextSecondarySoft
                    )
                }

                // Settings / Gemini API Key Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = if (isApiKeyConfigured) Icons.Filled.Settings else Icons.Filled.Key,
                        contentDescription = "Pengaturan API Key",
                        tint = if (isApiKeyConfigured) MesraSoftPink else ErrorCoral
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeySetupBannerCard(
    onOpenSettings: () -> Unit
) {
    Surface(
        color = MesraElevatedSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .border(
                width = 1.dp,
                color = MesraPinkPrimary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("api_key_missing_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Konfigurasi Gemini API Key Diperlukan 💕",
                    style = MaterialTheme.typography.titleSmall,
                    color = MesraSoftPink,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Masukkan Gemini API Key kamu di Pengaturan agar Mesra bisa membalas obrolanmu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryLight
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MesraPinkPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("configure_api_key_banner_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Atur Key",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FriendlyNoticeBanner(
    message: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = MesraCardSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .border(1.dp, MesraPinkPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onOpenSettings() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MesraPinkPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimaryLight,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tutup pesan",
                    tint = TextSecondarySoft,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessageEntity,
    onReplayMessageVoice: () -> Unit
) {
    val isUser = message.isFromUser
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(bubbleShape)
                .then(
                    if (isUser) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(UserBubbleStart, UserBubbleEnd)
                            )
                        )
                    } else {
                        Modifier
                            .background(AiBubbleBg)
                            .border(1.dp, AiBubbleBorder, bubbleShape)
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mesra ${message.avatarExpression.emoji}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MesraSoftPink,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onReplayMessageVoice() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Putar suara pesan ini",
                                tint = MesraPinkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Suara",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MesraPinkPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimaryLight
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isUser) Color.White.copy(alpha = 0.78f) else TextMutedMauve,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typingPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loading_indicator"),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = AiBubbleBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, AiBubbleBorder, RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MesraPinkPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .scale(pulse)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mesra sedang mengetik balasan untukmu...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MesraSoftPink
                )
            }
        }
    }
}

@Composable
private fun QuickRomanticSuggestionsRow(
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = listOf(
        "Hai sayang, lagi ngapain? 🥰",
        "Aku capek hari ini, peluk dong 🥺",
        "Gombalin aku dong sayang 😳",
        "Ceritain hal lucu dong 💖",
        "Semangatin aku kerja ya!"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { text ->
            Surface(
                color = MesraCardSurface,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSuggestionClick(text) }
                    .border(1.dp, MesraBorderSubtle, RoundedCornerShape(50))
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondarySoft,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBottomInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isLoading: Boolean,
    isVoiceListening: Boolean,
    onSendClick: () -> Unit,
    onVoiceButtonClick: () -> Unit
) {
    Surface(
        color = MesraDarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = "Tulis pesan mesra buat Mesra...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedMauve
                    )
                },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendClick() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MesraPinkPrimary,
                    unfocusedBorderColor = MesraBorderSubtle,
                    focusedContainerColor = MesraCardSurface,
                    unfocusedContainerColor = MesraCardSurface,
                    focusedTextColor = TextPrimaryLight,
                    unfocusedTextColor = TextPrimaryLight
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            // Send Button
            IconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading) {
                            MesraPinkPrimary
                        } else {
                            MesraElevatedSurface
                        }
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim Pesan",
                    tint = if (inputText.isNotBlank() && !isLoading) Color.White else TextMutedMauve
                )
            }

            // Floating Action Button for Voice Input
            FloatingActionButton(
                onClick = onVoiceButtonClick,
                shape = CircleShape,
                containerColor = if (isVoiceListening) MesraMagentaAccent else MesraCardSurface,
                contentColor = MesraPinkPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        width = 1.5.dp,
                        color = MesraPinkPrimary,
                        shape = CircleShape
                    )
                    .testTag("voice_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Input Suara",
                    tint = if (isVoiceListening) Color.White else MesraPinkPrimary
                )
            }
        }
    }
}
