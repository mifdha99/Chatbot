package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.preferences.CompanionMemoryState
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.MesraBorderSubtle
import com.example.ui.theme.MesraCardSurface
import com.example.ui.theme.MesraDarkBg
import com.example.ui.theme.MesraElevatedSurface
import com.example.ui.theme.MesraPinkPrimary
import com.example.ui.theme.MesraSoftPink
import com.example.ui.theme.OnlineMint
import com.example.ui.theme.TextMutedMauve
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondarySoft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiKeySettingsDialog(
    currentApiKey: String,
    isUsingBuildConfigFallback: Boolean,
    selectedModel: String,
    isTtsEnabled: Boolean,
    onSaveSettings: (apiKey: String, model: String, ttsEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var modelInput by remember(selectedModel) { mutableStateOf(selectedModel) }
    var ttsState by remember(isTtsEnabled) { mutableStateOf(isTtsEnabled) }
    var showApiKey by remember { mutableStateOf(false) }

    val availableModels = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Cepat & Stabil)",
        "gemini-flash-latest" to "Gemini Flash Latest (Otomatis)",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Mendalam)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_settings_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MesraCardSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .border(
                        width = 1.dp,
                        color = MesraBorderSubtle,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MesraPinkPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = MesraPinkPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Pengaturan & Gemini API",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "Simpan kunci API secara lokal & aman di HP kamu",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySoft
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.labelLarge,
                    color = MesraSoftPink
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = {
                        Text(
                            text = if (isUsingBuildConfigFallback) {
                                "Menggunakan API Key bawaan environment"
                            } else {
                                "Tempelkan AIzaSy... di sini"
                            },
                            color = TextMutedMauve
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Tampilkan/Sembunyikan API Key",
                                tint = TextSecondarySoft
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MesraPinkPrimary,
                        unfocusedBorderColor = MesraBorderSubtle,
                        focusedContainerColor = MesraDarkBg,
                        unfocusedContainerColor = MesraDarkBg,
                        focusedTextColor = TextPrimaryLight,
                        unfocusedTextColor = TextPrimaryLight
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field")
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tips: Kamu bisa mendapatkan Gemini API Key gratis dari Google AI Studio (aistudio.google.com). Kunci hanya disimpan di perangkatmu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySoft
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pilih Model Gemini",
                    style = MaterialTheme.typography.labelLarge,
                    color = MesraSoftPink
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableModels.forEach { (modelId, label) ->
                        val isSelected = modelInput == modelId
                        Surface(
                            color = if (isSelected) MesraPinkPrimary.copy(alpha = 0.22f) else MesraDarkBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { modelInput = modelId }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MesraPinkPrimary else MesraBorderSubtle.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryLight
                                    )
                                    Text(
                                        text = modelId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySoft
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MesraPinkPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TTS Voice Switch
                Surface(
                    color = MesraDarkBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Suara Pasangan (TTS id-ID)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "Bacakan jawaban Mesra & gerakkan lip-sync otomatis",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySoft
                            )
                        }
                        Switch(
                            checked = ttsState,
                            onCheckedChange = { ttsState = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MesraPinkPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondarySoft)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveSettings(apiKeyInput.trim(), modelInput, ttsState)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MesraPinkPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text("Simpan Pengaturan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionMemoryDialog(
    memory: CompanionMemoryState,
    onSaveMemory: (userName: String, nickname: String, style: String, facts: List<String>) -> Unit,
    onResetMemory: () -> Unit,
    onDismiss: () -> Unit
) {
    var userName by remember(memory) { mutableStateOf(memory.userName) }
    var userNickname by remember(memory) { mutableStateOf(memory.userNickname) }
    var conversationStyle by remember(memory) { mutableStateOf(memory.conversationStyle) }
    var factsText by remember(memory) { mutableStateOf(memory.memoryFacts.joinToString("\n")) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    val presetStyles = listOf(
        "Hangat, romantis, sedikit manja, & natural",
        "Sangat manja, gombal, & penuh perhatian",
        "Santai, suka bercanda, & suportif",
        "Singkat, manis, & menenangkan"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("companion_memory_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MesraCardSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, MesraBorderSubtle, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MesraPinkPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = MesraPinkPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Memory Pasangan (Lokal)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "Ingatan Mesra tentang kamu & gaya obrolan",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySoft
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nama Kamu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MesraSoftPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    placeholder = { Text("Contoh: Rian / Dinda (otomatis terisi saat chat)", color = TextMutedMauve) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MesraPinkPrimary,
                        unfocusedBorderColor = MesraBorderSubtle,
                        focusedContainerColor = MesraDarkBg,
                        unfocusedContainerColor = MesraDarkBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_user_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Panggilan Kesayangan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MesraSoftPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = userNickname,
                    onValueChange = { userNickname = it },
                    placeholder = { Text("Contoh: Sayang, Ayang, Cinta, Mas", color = TextMutedMauve) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MesraPinkPrimary,
                        unfocusedBorderColor = MesraBorderSubtle,
                        focusedContainerColor = MesraDarkBg,
                        unfocusedContainerColor = MesraDarkBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_nickname_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Preferensi Gaya Percakapan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MesraSoftPink
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetStyles.forEach { preset ->
                        val selected = conversationStyle == preset
                        Surface(
                            color = if (selected) MesraPinkPrimary.copy(alpha = 0.25f) else MesraDarkBg,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { conversationStyle = preset }
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MesraPinkPrimary else MesraBorderSubtle,
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) TextPrimaryLight else TextSecondarySoft,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Catatan Konteks & Hal yang Diingat (1 per baris)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MesraSoftPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = factsText,
                    onValueChange = { factsText = it },
                    placeholder = {
                        Text(
                            "Contoh:\nSuka kopi susu gula aren\nSering lembur malam",
                            color = TextMutedMauve
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MesraPinkPrimary,
                        unfocusedBorderColor = MesraBorderSubtle,
                        focusedContainerColor = MesraDarkBg,
                        unfocusedContainerColor = MesraDarkBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Prominent Reset Memory Button as required by specification
                OutlinedButton(
                    onClick = { showResetConfirmation = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_memory_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Memory", fontWeight = FontWeight.SemiBold)
                }

                if (showResetConfirmation) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MesraDarkBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ErrorCoral.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Hapus semua ingatan nama, panggilan, dan preferensi kembali ke awal?",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showResetConfirmation = false }) {
                                    Text("Batal", color = TextSecondarySoft)
                                }
                                Button(
                                    onClick = {
                                        onResetMemory()
                                        showResetConfirmation = false
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ErrorCoral,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("confirm_reset_memory_button")
                                ) {
                                    Text("Ya, Reset Memory", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = TextSecondarySoft)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedFacts = factsText.lines()
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            onSaveMemory(userName, userNickname, conversationStyle, parsedFacts)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MesraPinkPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("save_memory_button")
                    ) {
                        Text("Simpan Memory", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ClearChatConfirmDialog(
    onConfirmClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MesraCardSurface,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = ErrorCoral
            )
        },
        title = {
            Text(
                text = "Bersihkan Riwayat Obrolan?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimaryLight
            )
        },
        text = {
            Text(
                text = "Semua bubble percakapan akan dihapus dari layar, namun Memory Pasangan (nama & panggilan sayangmu) tetap tersimpan.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondarySoft
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmClear()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorCoral,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("confirm_clear_chat_button")
            ) {
                Text("Hapus Chat", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondarySoft)
            }
        }
    )
}

@Composable
fun VoicePromptDialog(
    isListening: Boolean,
    spokenPartialText: String,
    voiceErrorText: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendVoicePhrase: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val quickVoicePhrases = listOf(
        "Hai sayang, kamu lagi ngapain sekarang? 🥰",
        "Aku capek banget hari ini, semangatin aku dong sayang 💕",
        "Gombalin aku dong biar aku senyum-senyum sendiri 😳",
        "Aku kangen banget ngobrol sama kamu Mesra 💖"
    )

    Dialog(onDismissRequest = {
        onStopListening()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_input_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MesraCardSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MesraBorderSubtle, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) MesraPinkPrimary.copy(alpha = 0.28f)
                            else MesraElevatedSurface
                        )
                        .border(
                            width = 2.dp,
                            color = if (isListening) MesraPinkPrimary else MesraBorderSubtle,
                            shape = CircleShape
                        )
                        .clickable {
                            if (isListening) onStopListening() else onStartListening()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Mulai Bicara",
                        tint = if (isListening) MesraPinkPrimary else MesraSoftPink,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isListening) "Mendengarkan suaramu (id-ID)..." else "Bicara Langsung ke Mesra",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isListening) OnlineMint else TextPrimaryLight
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        spokenPartialText.isNotBlank() -> "\"$spokenPartialText\""
                        voiceErrorText != null -> voiceErrorText
                        else -> "Ketuk ikon mikrofon untuk bicara, atau pilih pesan suara cepat di bawah:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySoft
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickVoicePhrases.forEach { phrase ->
                        Surface(
                            color = MesraDarkBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onStopListening()
                                    onSendVoicePhrase(phrase)
                                    onDismiss()
                                }
                                .border(1.dp, MesraBorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = MesraPinkPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryLight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onStopListening()
                        onDismiss()
                    }) {
                        Text("Tutup", color = TextSecondarySoft)
                    }
                }
            }
        }
    }
}
