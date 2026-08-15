package com.sotext.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val widgetStateManager: WidgetStateManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Force an update of the widget
        widgetStateManager.requestWidgetUpdate()
        return Result.success()
    }
}
