package dev.pocketdemo

import dev.pocketdemo.domain.*
import org.junit.Assert.*
import org.junit.Test

class CategoryTest {
    private fun tx(id: String, amount: Long, category: Category) = Transaction(id, "Fiction", amount, category, "2026-11-01")
    @Test fun emptyLedgerHasNoSpending() { assertTrue(spendingByCategory(emptyList()).isEmpty()) }
    @Test fun incomeAndZeroDoNotCountAsSpending() {
        assertTrue(spendingByCategory(listOf(tx("a", 100, Category.INCOME), tx("b", 0, Category.FOOD))).isEmpty())
    }
    @Test fun combinesDebitsWithinCategoryAndExcludesRefund() {
        assertEquals(listOf(Spending(Category.FOOD, 350)), spendingByCategory(listOf(
            tx("a", -100, Category.FOOD), tx("b", -250, Category.FOOD), tx("c", 80, Category.FOOD))))
    }
    @Test fun sortsLargestFirstWithStableTies() {
        assertEquals(listOf(Category.HOME, Category.FOOD, Category.GROCERIES), spendingByCategory(listOf(
            tx("a", -100, Category.GROCERIES), tx("b", -100, Category.FOOD), tx("c", -900, Category.HOME))).map { it.category })
    }
    @Test fun transfersAreExplicitlyIncluded() {
        assertEquals(500L, spendingByCategory(listOf(tx("a", -500, Category.TRANSFER))).single().cents)
    }
    @Test fun usesLongCentsRatherThanFloatingPointOrInt() {
        assertEquals(6_000_000_000L, spendingByCategory(listOf(tx("a", -3_000_000_000L, Category.HOME), tx("b", -3_000_000_000L, Category.HOME))).single().cents)
    }
}
