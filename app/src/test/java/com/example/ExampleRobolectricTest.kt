package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AvatarExpression
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MesraAI", appName)
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
