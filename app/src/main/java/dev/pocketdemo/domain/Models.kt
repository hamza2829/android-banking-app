package dev.pocketdemo.domain

import java.math.BigDecimal
import java.util.Locale

enum class Category(val title: String) {
    HOME("Home"), GROCERIES("Groceries"), FOOD("Food & drink"),
    TRANSPORT("Transport"), SHOPPING("Shopping"), TRANSFER("Transfers"), INCOME("Income")
}
data class Transaction(
    val id: String, val merchant: String, val amountCents: Long,
    val category: Category, val date: String, val note: String = "", val iban: String? = null,
)
data class Ledger(val balanceCents: Long, val transactions: List<Transaction>)
data class Spending(val category: Category, val cents: Long)

fun spendingByCategory(transactions: List<Transaction>): List<Spending> = transactions
    .filter { it.amountCents < 0 }
    .groupBy { it.category }
    .map { (category, items) -> Spending(category, items.sumOf { -it.amountCents }) }
    .sortedWith(compareByDescending<Spending> { it.cents }.thenBy { it.category.name })

object TransferValidation {
    const val MAX_CENTS = 1_000_000L
    // Deliberately limited demo coverage. This is not an IBAN registry or an account lookup.
    private val lengths = mapOf("DE" to 22, "GB" to 22, "FR" to 27, "NL" to 18, "ES" to 24, "IT" to 27, "BE" to 16, "AT" to 20, "CH" to 21)
    fun normalizeIban(value: String) = value.filterNot { it.isWhitespace() }.uppercase(Locale.ROOT)
    fun validIban(value: String): Boolean {
        val iban = normalizeIban(value)
        if (!iban.matches(Regex("[A-Z]{2}[0-9]{2}[A-Z0-9]+"))) return false
        if (lengths[iban.take(2)] != iban.length) return false
        var remainder = 0
        for (char in iban.drop(4) + iban.take(4)) {
            val digits = if (char in 'A'..'Z') (char - 'A' + 10).toString() else char.toString()
            for (digit in digits) remainder = (remainder * 10 + digit.digitToInt()) % 97
        }
        return remainder == 1
    }
    fun amountCents(value: String): Long? {
        if (!value.matches(Regex("[0-9]{1,5}(\\.[0-9]{1,2})?"))) return null
        return runCatching { BigDecimal(value).movePointRight(2).longValueExact() }.getOrNull()
            ?.takeIf { it in 1..MAX_CENTS }
    }
    fun amountError(value: String, balanceCents: Long): String? {
        val cents = amountCents(value) ?: return "Enter EUR 0.01–10,000.00, with up to 2 decimals."
        return if (cents > balanceCents) "Not enough demo funds." else null
    }
}
