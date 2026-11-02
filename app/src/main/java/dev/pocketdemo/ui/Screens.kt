package dev.pocketdemo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pocketdemo.domain.*
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF173D34)
private val Green = Color(0xFF196A55)
private val Muted = Color(0xFF61736B)
private val Cream = Color(0xFFF5F6EF)
private val Lime = Color(0xFFD5EDAA)
private val chartColors = listOf(Green, Color(0xFFABC47D), Color(0xFFE6AF68), Color(0xFF87B6AD), Color(0xFFA09EC4), Color(0xFFD48B7C))
fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
    currency = java.util.Currency.getInstance("EUR")
}.format(java.math.BigDecimal.valueOf(cents, 2))

@Composable
fun PocketApp(account: AccountViewModel, transfer: TransferViewModel, lock: LockViewModel,
              canLock: Boolean, authenticate: (String, (String) -> Unit) -> Unit) {
    val ledger by account.ledger.collectAsStateWithLifecycle()
    val spending by account.spending.collectAsStateWithLifecycle()
    val transferState by transfer.state.collectAsStateWithLifecycle()
    val locked by lock.locked.collectAsStateWithLifecycle()
    val enabled by lock.enabled.collectAsStateWithLifecycle()
    val error by account.error.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("Overview") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White,
        background = Cream, surface = Color.White, onSurface = Ink, secondaryContainer = Lime)) {
        Surface(Modifier.fillMaxSize(), color = Cream) {
            if (locked) {
                Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Lock, null, Modifier.size(44.dp), tint = Green)
                    Spacer(Modifier.height(24.dp))
                    Text("Your demo is locked", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Authenticate with your device to open the local ledger.", color = Muted)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { authenticate("unlock") { authError = it } }, modifier = Modifier.testTag("unlock")) { Text("Unlock demo") }
                    authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            } else {
                Scaffold(containerColor = Cream, bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        listOf("Overview" to Icons.Default.AccountBalanceWallet, "Spending" to Icons.Default.PieChart,
                            "Transfer" to Icons.AutoMirrored.Filled.Send, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                            NavigationBarItem(selected = page == name, onClick = { page = name; selected = null },
                                icon = { Icon(icon, null) }, label = { Text(name, fontSize = 11.sp) }, modifier = Modifier.testTag("nav$name"))
                        }
                    }
                }) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, null, tint = Green, modifier = Modifier.size(22.dp))
                            Text("  pocket", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Spacer(Modifier.weight(1f))
                            Surface(color = Lime, shape = RoundedCornerShape(20.dp)) {
                                Text("LOCAL DEMO", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (error != null) Text(error!!, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                        else if (ledger == null) CircularProgressIndicator(Modifier.padding(24.dp))
                        else {
                            val current = ledger!!
                            val detail = current.transactions.find { it.id == selected }
                            if (detail != null) TransactionDetail(detail) { selected = null }
                            else when (page) {
                                "Overview" -> OverviewScreen(current) { selected = it }
                                "Spending" -> SpendingScreen(spending)
                                "Transfer" -> TransferScreen(transferState, current.balanceCents, transfer::edit) { transfer.submit(current.balanceCents) }
                                else -> SettingsScreen(enabled, canLock, authError) { action -> authenticate(action) { authError = it } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun PageTitle(eyebrow: String, title: String, subtitle: String) {
    Text(eyebrow.uppercase(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(7.dp))
    Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 35.sp)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = Muted, fontSize = 13.sp)
}
@Composable
fun OverviewScreen(ledger: Ledger, select: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("transactionList"), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Your everyday, simplified", "A little more clarity.", "Welcome back, Alex · fictional account") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Available demo balance", color = Color(0xFFCADAD0), fontSize = 13.sp)
                        Spacer(Modifier.weight(1f)); Icon(Icons.Default.AccountBalanceWallet, null, tint = Lime)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(money(ledger.balanceCents), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("balance"))
                    Spacer(Modifier.height(22.dp))
                    Text("POCKET PERSONAL  /  EUR", color = Lime, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("Synthetic money. No real bank connection.", color = Color(0xFFCADAD0), fontSize = 11.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent activity", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f)); Text("${ledger.transactions.size} entries", color = Muted, fontSize = 12.sp)
            }
        }
        items(ledger.transactions, key = { it.id }) { transaction ->
            Card(onClick = { select(transaction.id) }, colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("transaction-${transaction.id}"), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Cream, shape = RoundedCornerShape(13.dp)) {
                        Icon(categoryIcon(transaction.category), null, Modifier.padding(12.dp).size(20.dp), tint = Green)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(transaction.merchant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${transaction.category.title} · ${transaction.date}", fontSize = 10.sp, color = Muted)
                    }
                    Text(money(transaction.amountCents), fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        color = if (transaction.amountCents > 0) Green else Ink)
                }
            }
        }
        item { Text("All names, balances and activity are fictional.", color = Muted, fontSize = 11.sp) }
    }
}
private fun categoryIcon(category: Category) = when(category) {
    Category.GROCERIES -> Icons.Default.ShoppingBasket
    Category.HOME -> Icons.Default.Home
    Category.FOOD -> Icons.Default.LocalCafe
    Category.TRANSPORT -> Icons.Default.Train
    Category.SHOPPING -> Icons.Default.ShoppingBag
    Category.TRANSFER -> Icons.AutoMirrored.Filled.Send
    Category.INCOME -> Icons.Default.SouthWest
}
@Composable fun TransactionDetail(transaction: Transaction, back: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = back, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text("Activity") }
        Spacer(Modifier.height(24.dp))
        Icon(categoryIcon(transaction.category), null, tint = Green, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(20.dp))
        PageTitle("Transaction details", transaction.merchant, "Local ledger entry · synthetic")
        Text(money(transaction.amountCents), fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 24.dp))
        DetailRow("Category", transaction.category.title)
        DetailRow("Date", transaction.date)
        DetailRow("Reference", transaction.note.ifBlank { "No reference" })
        transaction.iban?.let { DetailRow("Demo recipient IBAN", it) }
        DetailRow("Status", "Recorded locally only")
        DetailRow("Entry ID", transaction.id)
    }
}
@Composable private fun DetailRow(label: String, value: String) {
    Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
    Text(value, fontSize = 15.sp); HorizontalDivider(Modifier.padding(top = 14.dp), color = Cream)
}
@Composable fun SpendingScreen(spending: List<Spending>) {
    val total = spending.sumOf { it.cents }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        PageTitle("The bigger picture", "Where it went.", "All local activity · income excluded")
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(174.dp).semantics { contentDescription = "Spending by category. Total ${money(total)}. Category amounts are listed below." }) {
                        var start = -90f
                        spending.forEachIndexed { index, item ->
                            val sweep = if (total == 0L) 0f else item.cents.toFloat() / total * 360
                            drawArc(chartColors[index % chartColors.size], start + 1.5f, (sweep - 3f).coerceAtLeast(0f), false,
                                topLeft = Offset(12f, 12f), size = size.copy(width = size.width - 24f, height = size.height - 24f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 30f, cap = StrokeCap.Round))
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL SPENT", fontSize = 9.sp, letterSpacing = 1.sp, color = Muted)
                        Text(money(total), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("A view of your demo spending", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        spending.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(chartColors[index % chartColors.size], RoundedCornerShape(4.dp)))
                Text(item.category.title, Modifier.padding(start = 12.dp).weight(1f), fontSize = 14.sp)
                Text(money(item.cents), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (spending.isEmpty()) Text("No spending yet.")
    }
}
@Composable fun TransferScreen(state: TransferState, balance: Long, edit: (String, String) -> Unit, submit: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageTitle("Move demo money", "Send a little.", "A local simulation. No money leaves this device.")
        Text("Available ${money(balance)}", color = Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (state.sent) {
            Card(colors = CardDefaults.cardColors(containerColor = Lime), modifier = Modifier.testTag("transferSuccess")) {
                Text("Demo transfer recorded locally. No real payment was made.", Modifier.padding(18.dp))
            }
        }
        listOf("recipient" to "Recipient name", "iban" to "IBAN", "amount" to "Amount (EUR)", "note" to "Reference (optional)").forEach { (field, label) ->
            val value = when(field) { "recipient" -> state.recipient; "iban" -> state.iban; "amount" -> state.amount; else -> state.note }
            OutlinedTextField(value, { edit(field, it) }, label = { Text(label) },
                enabled = !state.busy, singleLine = true, isError = field in state.errors,
                supportingText = state.errors[field]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = if (field == "amount") KeyboardType.Decimal else KeyboardType.Text),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag(field))
        }
        state.failure?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("transferFailure")) }
        Button(onClick = submit, enabled = !state.busy && !state.sent, shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("sendTransfer")) {
            Text(if (state.busy) "Recording…" else "Record demo transfer")
        }
        Text("EUR 0.01–10,000.00 per transfer, within available funds. Use a dot for decimals. IBAN countries: DE, GB, FR, NL, ES, IT, BE, AT, CH. Format checks do not verify an account exists.", color = Muted, fontSize = 11.sp)
    }
}
@Composable private fun SettingsScreen(enabled: Boolean, available: Boolean, error: String?, authenticate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        PageTitle("Make yourself at home", "Your preferences.", "Pocket Demo · version 1.0")
        Spacer(Modifier.height(26.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp)) {
                Text("Device screen lock", fontWeight = FontWeight.Bold)
                Text(if (enabled) "On · locks whenever the app leaves the foreground." else "Off · use biometrics or your device PIN.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { authenticate(if (enabled) "disable" else "enable") }, enabled = available,
                    modifier = Modifier.testTag("toggleLock")) { Text(if (enabled) "Turn lock off" else "Enable screen lock") }
                if (!available) Text("Set up a secure device PIN or biometrics first. Reopen the app after setup.", fontSize = 12.sp)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        DetailRow("Data", "Synthetic records stored locally in Room.")
        DetailRow("Mock session", "Random local token encrypted with an Android Keystore backed key. It authorizes nothing.")
        DetailRow("Connections", "No bank API. No analytics. No payment network.")
        DetailRow("A small, honest demo", "Not a bank. No PSD2 integration, production security review, or Google Play release. Use fictional details only.")
    }
}
