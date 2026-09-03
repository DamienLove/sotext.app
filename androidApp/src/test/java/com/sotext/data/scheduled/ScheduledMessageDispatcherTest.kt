package com.sotext.data.scheduled

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.sotext.data.sms.MessageNotificationManager
import com.sotext.data.sms.SmsSender
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.model.ScheduledMessageStatus
import com.sotext.domain.repository.ScheduledMessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * [ScheduledMessageDispatcher] with everything it talks to faked, focused on the guarantees the
 * feature actually depends on: send success/failure paths, the CAS-backed double-send guard under
 * real concurrency, and recurring series not double-spawning.
 */
class ScheduledMessageDispatcherTest {

    private lateinit var context: Context
    private lateinit var repository: ScheduledMessageRepository
    private lateinit var smsSender: SmsSender
    private lateinit var alarmScheduler: ScheduledMessageAlarmScheduler
    private lateinit var dispatcher: ScheduledMessageDispatcher

    // A tiny in-memory fake backing store so claimForProcessing's CAS semantics are real, not
    // just "always returns true" - the double-send guard is the whole point of this class.
    private val store = mutableMapOf<Long, ScheduledMessage>()
    private val lock = Any()
    private var nextId = AtomicInteger(1)

    private fun baseMessage(recurring: Boolean = false) = ScheduledMessage(
        id = 0,
        address = "+15551234567",
        body = "Test message",
        scheduledForUtcMillis = System.currentTimeMillis() - 1000,
        timezoneId = "America/New_York",
        recurrenceRule = if (recurring) {
            com.sotext.domain.model.RecurrenceRule(frequency = com.sotext.domain.model.RecurrenceFrequency.DAILY)
        } else null
    )

    private fun seed(message: ScheduledMessage): Long {
        val id = nextId.getAndIncrement().toLong()
        store[id] = message.copy(id = id)
        return id
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        // normalizeSmsAddress() calls the real android.telephony.PhoneNumberUtils, which - like
        // every android.* class - is the unimplemented SDK stub jar in a plain JVM unit test and
        // throws on any call. Stubbed here as an identity pass-through purely so
        // ScheduledMessageDispatcher's address normalization doesn't blow up before ever reaching
        // SmsSender; the actual normalization logic isn't what's under test in this file.
        mockkStatic(PhoneNumberUtils::class)
        every { PhoneNumberUtils.normalizeNumber(any()) } answers { firstArg() }

        mockkObject(MessageNotificationManager)
        every { MessageNotificationManager.notifyScheduledSendFailed(any(), any()) } returns Unit

        context = mockk(relaxed = true)
        smsSender = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)

        repository = mockk()
        coEvery { repository.getById(any()) } answers { synchronized(lock) { store[firstArg()] } }
        coEvery { repository.claimForProcessing(any()) } coAnswers {
            val id: Long = firstArg()
            synchronized(lock) {
                val current = store[id] ?: return@coAnswers false
                if (current.status == ScheduledMessageStatus.SCHEDULED || current.status == ScheduledMessageStatus.FAILED) {
                    store[id] = current.copy(status = ScheduledMessageStatus.PROCESSING)
                    true
                } else {
                    false
                }
            }
        }
        coEvery { repository.markSent(any(), any()) } answers {
            val id: Long = firstArg()
            synchronized(lock) { store[id] = store.getValue(id).copy(status = ScheduledMessageStatus.SENT) }
        }
        coEvery { repository.markFailed(any(), any()) } answers {
            val id: Long = firstArg()
            val error: String = secondArg()
            synchronized(lock) {
                val current = store.getValue(id)
                store[id] = current.copy(status = ScheduledMessageStatus.FAILED, retryCount = current.retryCount + 1, lastError = error)
            }
        }
        coEvery { repository.getBySeriesAndTime(any(), any()) } returns null
        coEvery { repository.insert(any()) } answers { seed(firstArg()) }

        dispatcher = ScheduledMessageDispatcher(context, repository, smsSender, alarmScheduler)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(PhoneNumberUtils::class)
        unmockkObject(MessageNotificationManager)
    }

    @Test
    fun `successful send marks the row SENT`() = runTest {
        coEvery { smsSender.sendSms(any(), any(), any(), true) } returns true
        val id = seed(baseMessage())

        dispatcher.dispatch(id)

        assertEquals(ScheduledMessageStatus.SENT, store.getValue(id).status)
        coVerify(exactly = 1) { smsSender.sendSms("+15551234567", "Test message", any(), true) }
    }

    @Test
    fun `failed send marks the row FAILED and notifies`() = runTest {
        coEvery { smsSender.sendSms(any(), any(), any(), true) } returns false
        val id = seed(baseMessage())

        dispatcher.dispatch(id)

        assertEquals(ScheduledMessageStatus.FAILED, store.getValue(id).status)
        coVerify(exactly = 1) { MessageNotificationManager.notifyScheduledSendFailed(any(), any()) }
    }

    @Test
    fun `concurrent double-dispatch of the same id sends exactly once`() = runTest {
        coEvery { smsSender.sendSms(any(), any(), any(), true) } returns true
        val id = seed(baseMessage())

        // Mirrors the alarm and the sweep worker both landing on the same due row.
        listOf(async { dispatcher.dispatch(id) }, async { dispatcher.dispatch(id) }).awaitAll()

        coVerify(exactly = 1) { smsSender.sendSms(any(), any(), any(), true) }
        assertEquals(ScheduledMessageStatus.SENT, store.getValue(id).status)
    }

    @Test
    fun `an already-terminal row is never re-dispatched`() = runTest {
        val id = seed(baseMessage())
        store[id] = store.getValue(id).copy(status = ScheduledMessageStatus.CANCELLED)

        dispatcher.dispatch(id)

        coVerify(exactly = 0) { smsSender.sendSms(any(), any(), any(), any()) }
    }

    @Test
    fun `a completed recurring occurrence spawns exactly one next occurrence`() = runTest {
        coEvery { smsSender.sendSms(any(), any(), any(), true) } returns true
        val id = seed(baseMessage(recurring = true))

        dispatcher.dispatch(id)

        val spawned = store.values.filter { it.id != id }
        assertEquals("exactly one next occurrence should be spawned", 1, spawned.size)
        assertEquals(ScheduledMessageStatus.SCHEDULED, spawned.first().status)
        assertNotEquals("the new occurrence must have its own occurrenceKey", store.getValue(id).occurrenceKey, spawned.first().occurrenceKey)
        assertEquals("the new occurrence stays in the same series", store.getValue(id).seriesId, spawned.first().seriesId)
    }

    @Test
    fun `re-entering dispatch after a recurring send does not double-spawn the next occurrence`() = runTest {
        coEvery { smsSender.sendSms(any(), any(), any(), true) } returns true
        val id = seed(baseMessage(recurring = true))
        dispatcher.dispatch(id)
        val firstSpawnCount = store.size

        // Simulate a stale-PROCESSING reclaim re-driving dispatch() on the now-SENT row (should be
        // a no-op: claimForProcessing only succeeds from SCHEDULED/FAILED).
        dispatcher.dispatch(id)

        assertEquals("dispatching an already-SENT row must not spawn another occurrence", firstSpawnCount, store.size)
    }
}
