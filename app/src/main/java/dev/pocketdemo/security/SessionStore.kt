package dev.pocketdemo.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

interface SessionSettings {
    var lockEnabled: Boolean
    fun ensureMockToken(): String
}
@Suppress("DEPRECATION") // Explicit demo requirement; limitations and deprecation documented.
class EncryptedSessionStore(context: Context) : SessionSettings {
    private val prefs = EncryptedSharedPreferences.create(context, "demo_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    override var lockEnabled: Boolean
        get() = prefs.getBoolean("lock", false)
        set(value) { check(prefs.edit().putBoolean("lock", value).commit()) }
    override fun ensureMockToken(): String = prefs.getString("mock_token", null) ?: run {
        val token = "local-demo-" + UUID.randomUUID()
        check(prefs.edit().putString("mock_token", token).commit())
        token
    }
}
