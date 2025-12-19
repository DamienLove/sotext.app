package com.pulselink.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulselink.domain.model.EscalationTier
import com.pulselink.service.AlertRouter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.glance.action.ActionParameters

@HiltWorker
class EmergencyWidgetActionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val alertRouter: AlertRouter
) : CoroutineWorker(context, workerParams) {

    companion object {
        val ACTION_KEY = ActionParameters.Key<String>("action")
        const val ACTION_EMERGENCY = "emergency"
        const val ACTION_CHECK_IN = "check_in"
    }

    override suspend fun doWork(): Result {
        val action = inputData.getString(ACTION_KEY.name)

        return when (action) {
            ACTION_EMERGENCY -> {
                alertRouter.dispatchManual(EscalationTier.EMERGENCY, "Widget trigger")
                Result.success()
            }
            ACTION_CHECK_IN -> {
                alertRouter.dispatchManual(EscalationTier.CHECK_IN, "Widget trigger")
                Result.success()
            }
            else -> Result.failure()
        }
    }
}
