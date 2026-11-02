package dev.pocketdemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pocketdemo.data.BankRepository
import dev.pocketdemo.domain.*
import dev.pocketdemo.security.SessionSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(repository: BankRepository) : ViewModel() {
    val ledger = repository.ledger.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val spending = ledger.map { spendingByCategory(it?.transactions.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val error = MutableStateFlow<String?>(null)
    init {
        viewModelScope.launch {
            try { repository.initialize() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = "Could not open the local demo ledger." }
        }
    }
}
data class TransferState(
    val recipient: String = "", val iban: String = "", val amount: String = "", val note: String = "",
    val errors: Map<String, String> = emptyMap(), val busy: Boolean = false, val sent: Boolean = false,
    val failure: String? = null,
)
class TransferViewModel(private val repository: BankRepository) : ViewModel() {
    private val _state = MutableStateFlow(TransferState())
    val state = _state.asStateFlow()
    fun edit(field: String, value: String) {
        if (_state.value.busy) return
        _state.update { s ->
            val next = when (field) {
                "recipient" -> s.copy(recipient = value.take(100))
                "iban" -> s.copy(iban = value.take(50))
                "amount" -> s.copy(amount = value.take(20))
                "note" -> s.copy(note = value.take(200))
                else -> s
            }
            next.copy(errors = next.errors - field, sent = false, failure = null)
        }
    }
    fun submit(balance: Long) {
        val s = _state.value
        if (s.busy || s.sent) return
        val errors = buildMap {
            if (s.recipient.trim().length !in 2..60) put("recipient", "Enter a name (2–60 characters).")
            if (!TransferValidation.validIban(s.iban)) put("iban", "Check the supported country, length and IBAN checksum.")
            TransferValidation.amountError(s.amount, balance)?.let { put("amount", it) }
            if (s.note.length > 140) put("note", "Use 140 characters or fewer.")
        }
        _state.value = s.copy(errors = errors)
        if (errors.isNotEmpty()) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.transfer(s.recipient, s.iban, s.amount, s.note)
                _state.value = TransferState(sent = true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(busy = false, failure = e.message ?: "Demo transfer failed.") } }
        }
    }
}
class LockViewModel(private val settings: SessionSettings) : ViewModel() {
    val enabled = MutableStateFlow(settings.lockEnabled)
    val locked = MutableStateFlow(settings.lockEnabled)
    fun authenticated(enable: Boolean = false) {
        if (enable) { settings.lockEnabled = true; enabled.value = true }
        locked.value = false
    }
    fun backgrounded() { if (enabled.value) locked.value = true }
    fun disableAfterAuthentication() {
        settings.lockEnabled = false
        enabled.value = false
        locked.value = false
    }
}
