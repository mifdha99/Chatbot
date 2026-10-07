package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MesraDatabase
import com.example.data.model.AvatarExpression
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.GeminiNetworkClient
import com.example.data.repository.ChatRepository
import com.example.voice.TtsLipSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `launch MainActivity and verify UI and dialogs render without crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MesraAI", appName)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("avatar_stage_card").assertExists()
        composeTestRule.onNodeWithTag("chat_input_field").assertExists()

        // Test opening Settings dialog
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("api_key_settings_dialog").assertExists()
    }

    @Test
    fun `tts manager speaks without throwing regex or runtime exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ttsManager = TtsLipSyncManager(context)
        ttsManager.speak("[SMILE] Hai sayang 🥰 gimana kabarmu hari ini? *peluk hangat*", isVoiceEnabled = true)
        ttsManager.stopSpeaking()
        ttsManager.shutdown()
    }

    @Test
    fun `parseReplyAndExpression extracts tag and cleans message`() {
        val (text, expr) = AvatarExpression.parseReplyAndExpression(
            "[SHY] Ih kamu bisa aja gombalnya sayang 😳"
        )
        assertEquals("Ih kamu bisa aja gombalnya sayang 😳", text)
        assertEquals(AvatarExpression.SHY, expr)
    }

    @Test
    fun `streaming chunk parser hides incomplete tag and sanitizes API keys`() {
        val (incompleteText, _) = AvatarExpression.parseStreamingChunk("[SMI")
        assertEquals("", incompleteText)

        val (partialText, partialExpr) = AvatarExpression.parseStreamingChunk("[HAPPY] Aku senang banget")
        assertEquals("Aku senang banget", partialText)
        assertEquals(AvatarExpression.HAPPY, partialExpr)

        val sanitized = GeminiNetworkClient.sanitizeSensitiveText(
            "Error calling https://generativelanguage.googleapis.com/?key=AIzaSyTestSecretKey1234567890",
            "AIzaSyTestSecretKey1234567890"
        )
        assertFalse(sanitized.contains("AIzaSyTestSecretKey1234567890"))
    }

    @Test
    fun `buildValidGeminiContents normalizes leading model welcome message and consecutive user messages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ChatRepository(
            MesraDatabase.getDatabase(context).chatDao(),
            UserPreferencesRepository.getInstance(context)
        )
        val history = listOf(
            ChatMessageEntity(id = 1, text = "Hai sayang... Aku Mesra", isFromUser = false),
            ChatMessageEntity(id = 2, text = "Pesan gagal sebelumnya", isFromUser = true),
            ChatMessageEntity(id = 3, text = "Halo Mesra sayang", isFromUser = true)
        )
        val contents = repo.buildValidGeminiContents(history, "Halo Mesra sayang")
        assertEquals(1, contents.size)
        assertEquals("user", contents.first().role)
        assertEquals("Halo Mesra sayang", contents.first().parts?.first()?.text)
    }
}
