package com.pulselink.data.sms

import com.pulselink.domain.model.PulseLinkSettings
import android.util.Log
import com.pulselink.domain.repository.SettingsRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsSyncManagerTest {

    private lateinit var smsRepository: SmsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var smsSyncTrigger: SmsSyncTrigger
    private lateinit var smsSyncManager: SmsSyncManager

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val changesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val settingsFlow = MutableStateFlow(PulseLinkSettings())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        smsRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        smsSyncTrigger = mockk(relaxed = true)

        every { smsRepository.changes() } returns changesFlow
        every { settingsRepository.settings } returns settingsFlow

        smsSyncManager = SmsSyncManager(smsRepository, smsSyncTrigger, settingsRepository)
        // Inject test scope sharing the same dispatcher
        smsSyncManager.scope = CoroutineScope(testDispatcher)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `when settings change to enable remoteWebAccess, sync is triggered`() = testScope.runTest {
        // Initial state: disabled
        settingsFlow.value = PulseLinkSettings(remoteWebAccessEnabled = false)

        smsSyncManager.start()
        advanceUntilIdle()

        // Enable it
        settingsFlow.value = PulseLinkSettings(remoteWebAccessEnabled = true)
        advanceUntilIdle()

        verify(atLeast = 1) { smsSyncTrigger.triggerSync() }
    }

    @Test
    fun `when upgrading to premium, remoteWebAccess is enabled automatically`() = testScope.runTest {
        // Initial state: free, disabled
        settingsFlow.value = PulseLinkSettings(
            premiumUnlocked = false,
            remoteWebAccessEnabled = false
        )

        smsSyncManager.start()
        advanceUntilIdle()

        // Upgrade to premium
        settingsFlow.value = PulseLinkSettings(
            premiumUnlocked = true,
            remoteWebAccessEnabled = false
        )
        advanceUntilIdle()

        // Verify that we tried to enable it in settings
        coVerify { settingsRepository.setRemoteWebAccessEnabled(true) }
    }

    @Test
    fun `when upgrading to premium, web access is enabled AND sync is triggered`() = testScope.runTest {
        // 1. Initial State: Free, Web Access Disabled
        val initialSettings = PulseLinkSettings(
            premiumUnlocked = false,
            remoteWebAccessEnabled = false
        )
        settingsFlow.value = initialSettings

        // Mock the update call to actually update the flow to simulate "setRemoteWebAccessEnabled" effect
        coEvery { settingsRepository.setRemoteWebAccessEnabled(true) } answers {
            // Simulate the repo updating the flow
            settingsFlow.value = settingsFlow.value.copy(remoteWebAccessEnabled = true)
        }

        smsSyncManager.start()
        advanceUntilIdle()

        // Clear initial interactions (e.g. initial sync if any)
        clearMocks(smsSyncTrigger)

        // 2. Action: User buys Premium
        // This updates the settings to Premium=true, Web=false (initially)
        settingsFlow.value = initialSettings.copy(premiumUnlocked = true)
        advanceUntilIdle()

        // 3. Verification
        // a) Should have called setRemoteWebAccessEnabled(true)
        coVerify { settingsRepository.setRemoteWebAccessEnabled(true) }

        // b) SmsSyncManager should have observed the change (Web: false -> true) and triggered sync
        // Note: The logic handles:
        // - Emission 1 (Premium=True, Web=False) -> calls setRemoteWebAccessEnabled(true)
        // - Emission 2 (Premium=True, Web=True) -> triggering sync because web changed from false to true
        verify { smsSyncTrigger.triggerSync() }
    }

    @Test
    fun `when sms repository changes, sync is triggered if enabled`() = testScope.runTest {
        // Enabled
        settingsFlow.value = PulseLinkSettings(remoteWebAccessEnabled = true)

        smsSyncManager.start()
        advanceUntilIdle()

        // Trigger change
        changesFlow.emit(Unit)

        // Advance time for debounce (500ms)
        advanceTimeBy(501L)
        advanceUntilIdle()

        verify(atLeast = 1) { smsSyncTrigger.triggerSync() }
    }

    @Test
    fun `when sms repository changes, sync is NOT triggered if disabled`() = testScope.runTest {
        // Disabled
        settingsFlow.value = PulseLinkSettings(remoteWebAccessEnabled = false)

        smsSyncManager.start()
        advanceUntilIdle()

        // Trigger change
        changesFlow.emit(Unit)

        // Advance time for debounce
        advanceTimeBy(600L)
        advanceUntilIdle()

        verify(exactly = 0) { smsSyncTrigger.triggerSync() }
    }
}
