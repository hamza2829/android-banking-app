package dev.pocketdemo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import android.content.Context
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

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
        // AGP uninstalls the app at teardown. A shell copy of synthetic screenshots survives it.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val exported = "/sdcard/Download/pocket-demo-screenshots/$name.png"
        device.executeShellCommand("mkdir -p /sdcard/Download/pocket-demo-screenshots")
        device.executeShellCommand("cp ${File(directory, "$name.png").absolutePath} $exported")
        assertTrue("Screenshot must be exported before teardown", (device.executeShellCommand("stat -c %s $exported").trim().toLongOrNull() ?: 0) > 0)
    }
}
