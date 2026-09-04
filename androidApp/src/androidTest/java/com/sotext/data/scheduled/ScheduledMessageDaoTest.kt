package com.sotext.data.scheduled

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sotext.data.db.PulseLinkDatabase
import com.sotext.data.db.ScheduledMessageDao
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.model.ScheduledMessageStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented (real SQLite, not mocked) tests for the CAS/idempotency semantics that make a
 * double-send impossible - see [ScheduledMessageDao.claimForProcessing] and
 * [ScheduledMessageDao.cancel]. These are exactly the primitives
 * [ScheduledMessageDispatcher.dispatch] and the alarm/sweep/manual-retry callers race against, so
 * this test exercises the real single-writer-connection SQLite serialization rather than a fake.
 */
@RunWith(AndroidJUnit4::class)
class ScheduledMessageDaoTest {

    private lateinit var database: PulseLinkDatabase
    private lateinit var dao: ScheduledMessageDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, PulseLinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.scheduledMessageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun newMessage(
        occurrenceKey: String = "occ-${System.nanoTime()}",
        status: ScheduledMessageStatus = ScheduledMessageStatus.SCHEDULED,
        scheduledForUtcMillis: Long = System.currentTimeMillis()
    ) = ScheduledMessage(
        occurrenceKey = occurrenceKey,
        address = "+15551234567",
        body = "Test",
        scheduledForUtcMillis = scheduledForUtcMillis,
        timezoneId = "America/New_York",
        status = status
    )

    @Test
    fun claimForProcessing_onlyOneOfManyConcurrentCallersWins() = runBlocking {
        val id = dao.insert(newMessage())

        // 20 coroutines racing to claim the same row, exactly mirroring an alarm-fire and a
        // sweep-worker pass (and a manual retry) all landing at once.
        val results = (1..20).map {
            async { dao.claimForProcessing(id, System.currentTimeMillis()) }
        }.awaitAll()

        val winners = results.count { it > 0 }
        assertEquals("exactly one caller should have won the claim", 1, winners)

        val row = dao.getById(id)
        assertEquals(ScheduledMessageStatus.PROCESSING, row?.status)
    }

    @Test
    fun claimForProcessing_failsOnAlreadyProcessingRow() = runBlocking {
        val id = dao.insert(newMessage())
        assertTrue(dao.claimForProcessing(id, System.currentTimeMillis()) > 0)
        // Second claim attempt on the now-PROCESSING row must be rejected.
        assertEquals(0, dao.claimForProcessing(id, System.currentTimeMillis()))
    }

    @Test
    fun claimForProcessing_succeedsFromFailed_forRetry() = runBlocking {
        val id = dao.insert(newMessage())
        dao.markFailed(id, "no service", System.currentTimeMillis())
        assertEquals(ScheduledMessageStatus.FAILED, dao.getById(id)?.status)

        assertTrue("retry/send-now must be able to claim a FAILED row", dao.claimForProcessing(id, System.currentTimeMillis()) > 0)
    }

    @Test
    fun claimForProcessing_failsOnTerminalRows() = runBlocking {
        val sentId = dao.insert(newMessage())
        dao.markSent(sentId, null, System.currentTimeMillis())
        assertEquals(0, dao.claimForProcessing(sentId, System.currentTimeMillis()))

        val cancelledId = dao.insert(newMessage())
        dao.cancel(cancelledId, System.currentTimeMillis())
        assertEquals(0, dao.claimForProcessing(cancelledId, System.currentTimeMillis()))
    }

    @Test
    fun concurrentCancelAndClaim_exactlyOneWins() = runBlocking {
        // Simulates the alarm firing the same instant the user taps Cancel.
        val id = dao.insert(newMessage())
        val now = System.currentTimeMillis()

        val claimResult = async { dao.claimForProcessing(id, now) }
        val cancelResult = async { dao.cancel(id, now) }
        val (claimed, cancelled) = listOf(claimResult, cancelResult).awaitAll()

        // Both operations guard on status='SCHEDULED', so SQLite's single-writer serialization
        // means exactly one of them observes the row still SCHEDULED and succeeds.
        val successes = listOf(claimed, cancelled).count { it > 0 }
        assertEquals(1, successes)

        val finalStatus = dao.getById(id)?.status
        assertTrue(finalStatus == ScheduledMessageStatus.PROCESSING || finalStatus == ScheduledMessageStatus.CANCELLED)
    }

    @Test
    fun cancel_neverSucceedsOnAProcessingRow() = runBlocking {
        val id = dao.insert(newMessage())
        assertTrue(dao.claimForProcessing(id, System.currentTimeMillis()) > 0)
        // A PROCESSING row must never be cancellable out from under an in-flight send.
        assertEquals(0, dao.cancel(id, System.currentTimeMillis()))
        assertEquals(ScheduledMessageStatus.PROCESSING, dao.getById(id)?.status)
    }

    @Test
    fun staleProcessing_isReclaimedForRetry() = runBlocking {
        val id = dao.insert(newMessage())
        dao.claimForProcessing(id, System.currentTimeMillis())

        val stale = dao.getStaleProcessing(System.currentTimeMillis() + 1)
        assertEquals(1, stale.size)
        assertEquals(id, stale.first().id)

        assertTrue(dao.reclaimStale(id, System.currentTimeMillis()) > 0)
        assertEquals(ScheduledMessageStatus.SCHEDULED, dao.getById(id)?.status)
        // Once reclaimed, it's claimable again - the retry path this exists for.
        assertTrue(dao.claimForProcessing(id, System.currentTimeMillis()) > 0)
    }

    @Test
    fun occurrenceKey_uniqueIndexRejectsDuplicateInserts() = runBlocking {
        dao.insert(newMessage(occurrenceKey = "dup-key"))
        try {
            dao.insert(newMessage(occurrenceKey = "dup-key"))
            fail("expected a unique-constraint violation on occurrenceKey")
        } catch (e: Exception) {
            // Expected: android.database.sqlite.SQLiteConstraintException (or Room's wrapper).
            assertTrue(e.javaClass.simpleName.contains("Constraint", ignoreCase = true) || e is android.database.sqlite.SQLiteConstraintException)
        }
    }

    @Test
    fun getDueForDispatch_onlyReturnsScheduledRowsAtOrBeforeNow() = runBlocking {
        val now = System.currentTimeMillis()
        val dueId = dao.insert(newMessage(scheduledForUtcMillis = now - 1000))
        val futureId = dao.insert(newMessage(scheduledForUtcMillis = now + 60_000))

        val due = dao.getDueForDispatch(now)
        assertTrue(due.any { it.id == dueId })
        assertTrue(due.none { it.id == futureId })
    }
}
