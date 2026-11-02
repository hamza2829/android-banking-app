package dev.pocketdemo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.io.File
import org.junit.Rule
import org.junit.Test

class ScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun captureThreeRealAppScreens() {
        compose.waitUntil(15000) { compose.onAllNodesWithTag("balance").fetchSemanticsNodes().isNotEmpty() }
        capture("overview")
        compose.onNodeWithTag("navSpending").performClick()
        compose.onNodeWithText("Where it went.").assertIsDisplayed(); capture("spending")
        compose.onNodeWithTag("navTransfer").performClick()
        compose.onNodeWithText("Send a little.").assertIsDisplayed(); capture("transfer")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
