package dev.pocketdemo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.pocketdemo.data.BankRepository
import dev.pocketdemo.data.DemoData
import dev.pocketdemo.domain.Ledger
import dev.pocketdemo.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*

class ComposeFormTest {
    @get:Rule val compose = createComposeRule()
    private var writes = 0
    private fun form() {
        val repository = object : BankRepository {
            override val ledger = MutableStateFlow<Ledger?>(Ledger(10000, emptyList()))
            override suspend fun initialize() = Unit
            override suspend fun transfer(recipient: String, iban: String, amount: String, note: String) { writes++ }
        }
        val vm = TransferViewModel(repository)
        compose.setContent { val state by vm.state.collectAsState(); MaterialTheme { TransferScreen(state, 10000, vm::edit) { vm.submit(10000) } } }
    }
    private fun fill(amount: String = "12.34") {
        compose.onNodeWithTag("recipient").performTextInput("Alex Demo")
        compose.onNodeWithTag("iban").performTextInput("DE89370400440532013000")
        compose.onNodeWithTag("amount").performTextInput(amount)
    }
    @Test fun emptyFormShowsErrorsAndDoesNotSubmit() {
        form(); compose.onNodeWithTag("sendTransfer").performScrollTo().performClick()
        compose.onNodeWithText("Enter a name (2–60 characters).").assertExists(); assertEquals(0, writes)
    }
    @Test fun invalidChecksumIsVisible() {
        form(); fill(); compose.onNodeWithTag("iban").performTextReplacement("DE88370400440532013000")
        compose.onNodeWithTag("sendTransfer").performScrollTo().performClick()
        compose.onNodeWithText("Check the supported country, length and IBAN checksum.").assertExists(); assertEquals(0, writes)
    }
    @Test fun amountOverBalanceCannotSubmit() {
        form(); fill("100.01"); compose.onNodeWithTag("sendTransfer").performScrollTo().performClick()
        compose.onNodeWithText("Not enough demo funds.").assertExists(); assertEquals(0, writes)
    }
    @Test fun validFormShowsLocalOnlyConfirmation() {
        form(); fill(); compose.onNodeWithTag("sendTransfer").performScrollTo().performClick()
        compose.onNodeWithTag("transferSuccess").assertExists(); assertEquals(1, writes)
        compose.onNodeWithTag("sendTransfer").assertIsNotEnabled()
    }
    @Test fun transactionListShowsSyntheticMerchantsAndOpensSelectedEntry() {
        var selected: String? = null
        compose.setContent { MaterialTheme { OverviewScreen(Ledger(DemoData.BALANCE, DemoData.transactions)) { selected = it } } }
        compose.onNodeWithTag("transactionList").performScrollToNode(hasTestTag("transaction-01"))
        compose.onNodeWithText("Sunday Market").assertIsDisplayed()
        compose.onNodeWithTag("transaction-01").performClick(); assertEquals("01", selected)
        compose.onNodeWithTag("transactionList").performScrollToNode(hasTestTag("transaction-06"))
        compose.onNodeWithText("Demo Salary").assertIsDisplayed()
    }
    @Test fun detailShowsActualSelectedData() {
        compose.setContent { MaterialTheme { TransactionDetail(DemoData.transactions.first()) {} } }
        compose.onNodeWithText("Sunday Market").assertIsDisplayed()
        compose.onNodeWithText("Weekly groceries · synthetic").assertExists()
    }
}
