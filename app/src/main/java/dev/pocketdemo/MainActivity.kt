package dev.pocketdemo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.pocketdemo.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private lateinit var lock: LockViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val app = application as PocketApplication
            val session = withContext(Dispatchers.IO) { app.session }
            lock = model(LockViewModel::class.java) { LockViewModel(session) }
            val account = model(AccountViewModel::class.java) { AccountViewModel(app.repository) }
            val transfer = model(TransferViewModel::class.java) { TransferViewModel(app.repository) }
            setContent {
                PocketApp(account, transfer, lock, canAuthenticate(), ::authenticate)
            }
        }
    }
    private fun canAuthenticate() = BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    private fun authenticate(action: String, onError: (String) -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                when (action) {
                    "disable" -> lock.disableAfterAuthentication()
                    "enable" -> lock.authenticated(enable = true)
                    else -> lock.authenticated()
                }
            }
            override fun onAuthenticationError(code: Int, message: CharSequence) { onError("Authentication cancelled or unavailable. Try again.") }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Pocket Demo")
            .setSubtitle("Use your device biometric or screen lock")
            .setAllowedAuthenticators(AUTHENTICATORS).build())
    }
    override fun onStop() {
        super.onStop()
        if (::lock.isInitialized) lock.backgrounded()
    }
    private fun <T : ViewModel> model(type: Class<T>, create: () -> T): T = ViewModelProvider(this,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
        })[type]
    companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
