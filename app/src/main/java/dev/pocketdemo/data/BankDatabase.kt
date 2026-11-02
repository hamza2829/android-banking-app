package dev.pocketdemo.data

import androidx.room.*
import dev.pocketdemo.domain.Category
import dev.pocketdemo.domain.Ledger
import dev.pocketdemo.domain.Transaction as BankTransaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accounts")
data class AccountEntity(@PrimaryKey val id: Int = 1, val balanceCents: Long)
@Entity(tableName = "transactions", indices = [Index("accountId")], foreignKeys = [ForeignKey(
    entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE
)])
data class TransactionEntity(
    @PrimaryKey val id: String, val accountId: Int = 1, val merchant: String,
    val amountCents: Long, val category: String, val date: String, val note: String, val iban: String? = null,
) {
    fun domain() = BankTransaction(id, merchant, amountCents, Category.valueOf(category), date, note, iban)
}
data class AccountLedger(
    @Embedded val account: AccountEntity,
    @Relation(parentColumn = "id", entityColumn = "accountId") val transactions: List<TransactionEntity>,
) {
    fun domain() = Ledger(account.balanceCents, transactions.map { it.domain() }
        .sortedWith(compareByDescending<BankTransaction> { it.date }.thenBy { it.id }))
}
@Dao
interface BankDao {
    @Transaction @Query("SELECT * FROM accounts WHERE id = 1")
    fun observe(): Flow<AccountLedger?>
    @Transaction @Query("SELECT * FROM accounts WHERE id = 1")
    suspend fun read(): AccountLedger?
    @Insert suspend fun insertAccount(account: AccountEntity)
    @Insert suspend fun insertTransactions(transactions: List<TransactionEntity>)
    @Query("UPDATE accounts SET balanceCents = balanceCents - :cents WHERE id = 1 AND balanceCents >= :cents AND :cents > 0")
    suspend fun debit(cents: Long): Int
}
@Database(entities = [AccountEntity::class, TransactionEntity::class], version = 1, exportSchema = true)
abstract class BankDatabase : RoomDatabase() { abstract fun dao(): BankDao }

object DemoData {
    const val BALANCE = 354580L
    val transactions = listOf(
        BankTransaction("01", "Sunday Market", -6840, Category.GROCERIES, "2026-11-01", "Weekly groceries · synthetic"),
        BankTransaction("02", "Little Roastery", -450, Category.FOOD, "2026-11-01", "Morning coffee · synthetic"),
        BankTransaction("03", "Metro Pass", -4900, Category.TRANSPORT, "2026-10-31", "Monthly pass · synthetic"),
        BankTransaction("04", "Studio Rent", -125000, Category.HOME, "2026-10-30", "November rent · synthetic"),
        BankTransaction("05", "Paper & Things", -3230, Category.SHOPPING, "2026-10-29", "Books and stationery · synthetic"),
        BankTransaction("06", "Demo Salary", 245000, Category.INCOME, "2026-10-28", "Fictional monthly income"),
    )
    fun entity(t: BankTransaction) = TransactionEntity(t.id, merchant = t.merchant, amountCents = t.amountCents,
        category = t.category.name, date = t.date, note = t.note, iban = t.iban)
}
