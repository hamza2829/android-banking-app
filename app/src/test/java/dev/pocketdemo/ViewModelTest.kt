package dev.pocketdemo

import dev.pocketdemo.data.BankRepository
import dev.pocketdemo.domain.*
import dev.pocketdemo.security.SessionSettings
import dev.pocketdemo.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(StandardTestDispatcher()) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
class FakeRepository : BankRepository {
    override val ledger = MutableStateFlow<Ledger?>(Ledger(10000, emptyList()))
    var transfers = 0
    var initFailure = false
    var transferFailure = false
    var waitForTransfer: CompletableDeferred<Unit>? = null
    override suspend fun initialize() { if (initFailure) error("Disk failure") }
    override suspend fun transfer(recipient: String, iban: String, amount: String, note: String) {
        transfers++
        waitForTransfer?.await()
        if (transferFailure) error("Not enough demo funds.")
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val repo = FakeRepository()
    private fun valid() = TransferViewModel(repo).apply {
        edit("recipient", "Alex Demo"); edit("iban", "DE89370400440532013000"); edit("amount", "12.34")
    }
    @Test fun emptyFormShowsErrorsWithoutWriting() = runTest {
        val vm = TransferViewModel(repo); vm.submit(10000); advanceUntilIdle()
        assertEquals(setOf("recipient", "iban", "amount"), vm.state.value.errors.keys); assertEquals(0, repo.transfers)
    }
    @Test fun validTransferWritesOnceAndClearsFields() = runTest {
        val vm = valid(); vm.submit(10000); advanceUntilIdle()
        assertTrue(vm.state.value.sent); assertEquals("", vm.state.value.iban); assertEquals(1, repo.transfers)
    }
    @Test fun amountCannotExceedBalance() = runTest {
        val vm = valid(); vm.submit(1233); advanceUntilIdle()
        assertEquals("Not enough demo funds.", vm.state.value.errors["amount"]); assertEquals(0, repo.transfers)
    }
    @Test fun amountMayEqualBalance() = runTest {
        val vm = valid(); vm.submit(1234); advanceUntilIdle(); assertTrue(vm.state.value.sent)
    }
    @Test fun duplicateTapsDoNotRecordTwice() = runTest {
        repo.waitForTransfer = CompletableDeferred()
        val vm = valid(); vm.submit(10000); vm.submit(10000); runCurrent()
        assertTrue(vm.state.value.busy); assertEquals(1, repo.transfers)
        repo.waitForTransfer!!.complete(Unit); advanceUntilIdle(); vm.submit(10000)
        assertEquals(1, repo.transfers)
    }
    @Test fun repositoryFailureIsVisibleAndKeepsInputForRetry() = runTest {
        repo.transferFailure = true; val vm = valid(); vm.submit(10000); advanceUntilIdle()
        assertFalse(vm.state.value.busy); assertFalse(vm.state.value.sent)
        assertEquals("12.34", vm.state.value.amount); assertEquals("Not enough demo funds.", vm.state.value.failure)
    }
    @Test fun correctingFieldClearsItsError() = runTest {
        val vm = TransferViewModel(repo); vm.submit(10000); vm.edit("iban", "DE89370400440532013000")
        assertFalse("iban" in vm.state.value.errors); assertTrue("amount" in vm.state.value.errors)
    }
    @Test fun referenceLimitIsEnforced() = runTest {
        val vm = valid(); vm.edit("note", "x".repeat(141)); vm.submit(10000); advanceUntilIdle()
        assertTrue("note" in vm.state.value.errors); assertEquals(0, repo.transfers)
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @Test fun observesLedgerAndCategoryChanges() = runTest {
        val repo = FakeRepository(); val vm = AccountViewModel(repo); advanceUntilIdle()
        assertEquals(10000L, vm.ledger.value!!.balanceCents)
        repo.ledger.value = Ledger(9800, listOf(Transaction("a", "Demo", -200, Category.FOOD, "2026-11-01")))
        advanceUntilIdle(); assertEquals(listOf(Spending(Category.FOOD, 200)), vm.spending.value)
    }
    @Test fun initializationFailureIsShown() = runTest {
        val vm = AccountViewModel(FakeRepository().apply { initFailure = true }); advanceUntilIdle()
        assertEquals("Could not open the local demo ledger.", vm.error.value)
    }
}
class LockViewModelTest {
    private class Settings(override var lockEnabled: Boolean = false) : SessionSettings {
        override fun ensureMockToken() = "fictional"
    }
    @Test fun enabledSessionStartsLocked() { assertTrue(LockViewModel(Settings(true)).locked.value) }
    @Test fun successfulAuthenticationEnablesAndUnlocks() {
        val s = Settings(); val vm = LockViewModel(s); vm.authenticated(true)
        assertTrue(s.lockEnabled); assertFalse(vm.locked.value)
    }
    @Test fun leavingForegroundRelocksEnabledSession() {
        val vm = LockViewModel(Settings(true)); vm.authenticated(); vm.backgrounded(); assertTrue(vm.locked.value)
    }
    @Test fun authenticatedDisablePersistsAndStopsRelocking() {
        val s = Settings(true); val vm = LockViewModel(s); vm.disableAfterAuthentication(); vm.backgrounded()
        assertFalse(s.lockEnabled); assertFalse(vm.locked.value)
    }
}
