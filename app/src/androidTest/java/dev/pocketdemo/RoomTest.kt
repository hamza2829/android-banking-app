package dev.pocketdemo

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import dev.pocketdemo.data.*
import dev.pocketdemo.domain.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTest {
    private lateinit var db: BankDatabase
    private lateinit var repo: LocalBankRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BankDatabase::class.java).build()
        repo = LocalBankRepository(db)
    }
    @After fun close() { db.close() }
    @Test fun seedIsIdempotentAndFlowContainsAllEntries() = runTest {
        repo.initialize(); repo.initialize()
        val ledger = repo.ledger.first()!!
        assertEquals(DemoData.BALANCE, ledger.balanceCents); assertEquals(6, ledger.transactions.size)
    }
    @Test fun transferAtomicallyDebitsAndAppends() = runTest {
        repo.initialize(); repo.transfer("Alex Demo", "DE89 3704 0044 0532 0130 00", "12.34", "Lunch")
        val ledger = db.dao().read()!!.domain()
        assertEquals(DemoData.BALANCE - 1234, ledger.balanceCents)
        val transfer = ledger.transactions.single { it.category == Category.TRANSFER }
        assertEquals(-1234L, transfer.amountCents); assertEquals("DE89370400440532013000", transfer.iban)
    }
    @Test fun insufficientFundsLeavesBalanceAndHistoryUnchanged() = runTest {
        repo.initialize()
        try { repo.transfer("Alex Demo", "DE89370400440532013000", "9999", ""); fail("Expected rejection") }
        catch (_: IllegalStateException) { }
        assertEquals(DemoData.BALANCE, db.dao().read()!!.account.balanceCents)
        assertEquals(6, db.dao().read()!!.transactions.size)
    }
    @Test fun invalidIbanCannotBypassViewModel() = runTest {
        repo.initialize()
        try { repo.transfer("Alex Demo", "DE000", "1", ""); fail("Expected rejection") }
        catch (_: IllegalArgumentException) { }
        assertEquals(6, db.dao().read()!!.transactions.size)
    }
    @Test fun concurrentTransfersCannotOverdraw() = runTest {
        repo.seedIfEmpty(100, emptyList())
        val attempts = (1..2).map { async { runCatching { repo.transfer("Demo Person", "DE89370400440532013000", "0.75", "") }.isSuccess } }.awaitAll()
        assertEquals(1, attempts.count { it }); assertEquals(25L, db.dao().read()!!.account.balanceCents)
        assertEquals(1, db.dao().read()!!.transactions.size)
    }
    @Test fun duplicateSeedIsRejectedBeforeWriting() = runTest {
        val t = DemoData.transactions.first()
        try { repo.seedIfEmpty(100, listOf(t, t)); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
        assertNull(db.dao().read())
    }
    @Test fun insertFailureRollsBackEarlierDebit() = runTest {
        repo.initialize()
        try {
            db.withTransaction {
                assertEquals(1, db.dao().debit(100))
                db.dao().insertTransactions(listOf(DemoData.entity(DemoData.transactions.first())))
            }
            fail("Expected duplicate primary key failure")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(DemoData.BALANCE, db.dao().read()!!.account.balanceCents)
        assertEquals(6, db.dao().read()!!.transactions.size)
    }
    @Test fun fileDatabaseSurvivesReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        try {
            Room.databaseBuilder(context, BankDatabase::class.java, name).build().let { disk ->
                try { LocalBankRepository(disk).initialize() } finally { disk.close() }
            }
            Room.databaseBuilder(context, BankDatabase::class.java, name).build().let { disk ->
                try { assertEquals(DemoData.BALANCE, disk.dao().read()!!.account.balanceCents) } finally { disk.close() }
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun retrofitMockSnapshotSeedsRoomOverLoopbackOnly() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"balanceCents":9876,"transactions":[]}""")); server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
            MockApi.importIntoEmptyDatabase(MockApi.create("http://127.0.0.1:${server.port}/"), repo)
            assertEquals(9876L, db.dao().read()!!.account.balanceCents)
            repo.initialize(); assertEquals(9876L, db.dao().read()!!.account.balanceCents)
        }
    }
}
