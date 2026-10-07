package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AvatarExpression
import com.example.voice.TtsLipSyncManager
import org.junit.Assert.assertEquals
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
}
