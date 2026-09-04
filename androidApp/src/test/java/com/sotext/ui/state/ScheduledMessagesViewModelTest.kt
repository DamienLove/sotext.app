package com.sotext.ui.state

import com.sotext.data.scheduled.ScheduledMessageAlarmScheduler
import com.sotext.data.scheduled.ScheduledMessageDispatcher
import com.sotext.domain.model.RecurrenceFrequency
import com.sotext.domain.model.RecurrenceRule
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.repository.ScheduledMessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * [ScheduledMessagesViewModel] backs the Scheduled hub - these tests cover the
 * cancel/send-now/retry/edit state-transition surface the hub's UI drives, with the repository,
 * alarm scheduler, and dispatcher faked so only the ViewModel's own wiring is under test.
 *
 * These calls are fire-and-forget (`viewModelScope.launch(Dispatchers.IO) { ... }`, matching how
 * a ViewModel should genuinely dispatch IO work in production) rather than suspend functions, so
 * this deliberately does NOT use `runTest`/a virtual-time `TestDispatcher` - that only controls
 * coroutines actually running on it, and these hop to the real `Dispatchers.IO`. Synchronization
 * instead uses MockK's real-timeout `coVerify(timeout = ...)`, which polls until the call happens
 * or the timeout elapses - the correct way to await a fire-and-forget coroutine on a real
 * dispatcher from a test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduledMessagesViewModelTest {

    private lateinit var repository: ScheduledMessageRepository
    private lateinit var alarmScheduler: ScheduledMessageAlarmScheduler
    private lateinit var dispatcher: ScheduledMessageDispatcher
    private lateinit var viewModel: ScheduledMessagesViewModel

    private fun sampleMessage(id: Long = 1L) = ScheduledMessage(
        id = id,
        address = "+15551234567",
        body = "Hello",
        scheduledForUtcMillis = System.currentTimeMillis() + 60_000,
        timezoneId = "America/New_York"
    )

    @Before
    fun setUp() {
        // viewModelScope's default context references Dispatchers.Main.immediate; every call in
        // this file explicitly overrides to Dispatchers.IO before anything actually dispatches,
        // but setting Main here removes any doubt about the "Main dispatcher missing" failure
        // mode plain JVM unit tests otherwise hit just from touching viewModelScope at all.
        Dispatchers.setMain(StandardTestDispatcher())
        repository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)
        dispatcher = mockk(relaxed = true)
        every { repository.observeUpcoming() } returns emptyFlow()
        viewModel = ScheduledMessagesViewModel(repository, alarmScheduler, dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancel only cancels the alarm when the repository cancel actually succeeded`() {
        coEvery { repository.cancel(1L) } returns true

        viewModel.cancel(1L)

        coVerify(timeout = 2000) { repository.cancel(1L) }
        coVerify(timeout = 2000) { alarmScheduler.cancel(1L) }
    }

    @Test
    fun `cancel on an already-terminal row does not touch the alarm`() {
        // e.g. the row already sent between the UI rendering it and the user tapping Cancel.
        coEvery { repository.cancel(1L) } returns false

        viewModel.cancel(1L)

        // Synchronize on the call we know happens, then assert the conditional one never did -
        // by the time repository.cancel's result has been observed, the very next line in the
        // same coroutine (the `if` branch) has already run or is about to on the same thread.
        coVerify(timeout = 2000) { repository.cancel(1L) }
        Thread.sleep(100)
        coVerify(exactly = 0) { alarmScheduler.cancel(any()) }
    }

    @Test
    fun `send now cancels the pending alarm and dispatches immediately`() {
        viewModel.sendNow(1L)

        coVerify(timeout = 2000) { alarmScheduler.cancel(1L) }
        coVerify(timeout = 2000) { dispatcher.sendNowManually(1L) }
    }

    @Test
    fun `retry is the same operation as send now`() {
        viewModel.retry(2L)

        coVerify(timeout = 2000) { alarmScheduler.cancel(2L) }
        coVerify(timeout = 2000) { dispatcher.sendNowManually(2L) }
    }

    @Test
    fun `update reschedules the alarm with the new time and never touches a different id`() {
        val existing = sampleMessage(id = 5L)
        coEvery { repository.getById(5L) } returns existing
        val slot = slot<ScheduledMessage>()
        coEvery { repository.update(capture(slot)) } returns Unit

        val newTime = existing.scheduledForUtcMillis + 3_600_000
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY)
        viewModel.update(5L, "Updated body", newTime, rule)

        coVerify(timeout = 2000) { alarmScheduler.scheduleExact(match { it.id == 5L && it.scheduledForUtcMillis == newTime }) }
        assertEquals("Updated body", slot.captured.body)
        assertEquals(newTime, slot.captured.scheduledForUtcMillis)
        assertEquals(rule, slot.captured.recurrenceRule)
        assertEquals(5L, slot.captured.id)
    }

    @Test
    fun `update against a row that no longer exists locally is a safe no-op`() {
        coEvery { repository.getById(99L) } returns null

        viewModel.update(99L, "body", System.currentTimeMillis(), null)

        coVerify(timeout = 2000) { repository.getById(99L) }
        Thread.sleep(100)
        coVerify(exactly = 0) { repository.update(any()) }
        coVerify(exactly = 0) { alarmScheduler.scheduleExact(any()) }
    }
}
