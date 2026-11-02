package dev.pocketdemo

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.pocketdemo.security.EncryptedSessionStore
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.regex.Pattern

class SessionEncryptionTest {
    @Test fun tokenSurvivesReopeningButIsAbsentFromPreferencesPlaintext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = EncryptedSessionStore(context).ensureMockToken()
        assertTrue(first.startsWith("local-demo-"))
        assertEquals(first, EncryptedSessionStore(context).ensureMockToken())
        val xml = File(context.applicationInfo.dataDir, "shared_prefs/demo_session.xml").readText()
        assertFalse(xml.contains(first)); assertFalse(xml.contains("mock_token"))
    }
}
class DeviceCredentialTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun realBiometricPromptAcceptsDevicePinAndRelocksAfterBackground() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // CI explicitly installs this emulator PIN; never use a personal device for this test.
        Assume.assumeTrue("Requires disposable emulator with PIN 1234", context.getSystemService(KeyguardManager::class.java).isDeviceSecure)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            compose.waitUntil(15000) { compose.onAllNodesWithTag("navSettings").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("navSettings").performClick()
            compose.onNodeWithTag("toggleLock").performClick()
            enterPin(device)
            compose.onNodeWithText("Turn lock off").assertExists()
            device.pressHome()
            device.executeShellCommand("am start -n dev.pocketdemo/.MainActivity")
            compose.waitUntil(10000) { compose.onAllNodesWithTag("unlock").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unlock").performClick(); enterPin(device)
            compose.onNodeWithTag("navSettings").assertExists()
            compose.onNodeWithTag("toggleLock").performClick(); enterPin(device)
            compose.onNodeWithText("Enable screen lock").assertExists()
        } finally {
            EncryptedSessionStore(context).lockEnabled = false
        }
    }
    private fun enterPin(device: UiDevice) {
        val selector = By.res(Pattern.compile(".*:id/(pinEntry|password_entry)"))
        val input = device.wait(Until.findObject(selector), 10000)
        assertNotNull("Android credential PIN field must appear", input)
        input.text = "1234"
        device.pressEnter()
        device.wait(Until.gone(selector), 10000)
    }
}
