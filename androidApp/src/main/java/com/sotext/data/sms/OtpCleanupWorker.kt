package com.sotext.data.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sotext.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class OtpCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val smsRepository: SmsRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.otpCleanupEnabled) return Result.success()
        val days = settings.otpCleanupDays.coerceAtLeast(1)
        val expiryMs = TimeUnit.DAYS.toMillis(days.toLong())
        smsRepository.purgeExpiredOtpMessages(expiryMs)
        return Result.success()
    }
}
