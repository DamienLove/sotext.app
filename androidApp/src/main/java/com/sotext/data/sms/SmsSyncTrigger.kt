package com.sotext.data.sms

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSyncTrigger @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun triggerSync(threadId: Long? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val builder = OneTimeWorkRequestBuilder<SmsSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)

        if (threadId != null) {
            val data = Data.Builder()
                .putLong("threadId", threadId)
                .build()
            builder.setInputData(data)
        }

        val syncRequest = builder.build()

        val uniqueWorkName = if (threadId != null) "SmsSyncThread_$threadId" else "SmsSyncFull"

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }
}
