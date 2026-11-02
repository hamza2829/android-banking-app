package dev.pocketdemo.data

import androidx.room.withTransaction
import dev.pocketdemo.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

interface BankRepository {
    val ledger: Flow<Ledger?>
    suspend fun initialize()
    suspend fun transfer(recipient: String, iban: String, amount: String, note: String)
}
class LocalBankRepository(private val db: BankDatabase) : BankRepository {
    override val ledger = db.dao().observe().map { it?.domain() }
    override suspend fun initialize() = seedIfEmpty(DemoData.BALANCE, DemoData.transactions)
    suspend fun seedIfEmpty(balance: Long, transactions: List<Transaction>) {
        require(balance in 0..100_000_000L)
        require(transactions.size <= 1000)
        require(transactions.map { it.id }.distinct().size == transactions.size)
        db.withTransaction {
            if (db.dao().read() == null) {
                db.dao().insertAccount(AccountEntity(balanceCents = balance))
                db.dao().insertTransactions(transactions.map(DemoData::entity))
            }
        }
    }
    override suspend fun transfer(recipient: String, iban: String, amount: String, note: String) {
        require(recipient.trim().length in 2..60) { "Enter a recipient name (2–60 characters)." }
        require(TransferValidation.validIban(iban)) { "Enter a supported IBAN with a valid checksum." }
        require(note.length <= 140) { "Reference must be 140 characters or fewer." }
        val cents = requireNotNull(TransferValidation.amountCents(amount)) { "Invalid demo amount." }
        db.withTransaction {
            check(db.dao().debit(cents) == 1) { "Not enough demo funds." }
            db.dao().insertTransactions(listOf(DemoData.entity(Transaction(
                UUID.randomUUID().toString(), recipient.trim(), -cents, Category.TRANSFER,
                LocalDate.now().toString(), note.trim(), TransferValidation.normalizeIban(iban)
            ))))
        }
    }
}
