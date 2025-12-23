package com.pulselink.data.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pulselink.BuildConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsRepository: SmsRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!BuildConfig.PREMIUM_FEATURES) {
            return Result.success()
        }

        val user = auth.currentUser
        if (user == null) {
            return Result.failure()
        }

        return try {
            val threads = smsRepository.listThreads(limit = 20)
            val threadsRef = firestore.collection("users").document(user.uid).collection("synced_threads")

            for (thread in threads) {
                val threadDoc = threadsRef.document(thread.threadId.toString())
                val threadData = mapOf(
                    "address" to thread.address,
                    "snippet" to thread.snippet,
                    "date" to thread.timestamp,
                    "unread" to thread.unread,
                    "isFavorite" to thread.isFavorite,
                    "isPrivate" to thread.isPrivate,
                    "isTrusted" to thread.isTrusted
                )
                // Write thread data
                threadDoc.set(threadData, SetOptions.merge()).await()

                // Sync messages
                val messages = smsRepository.messagesForThread(thread.threadId, limit = 50)
                val messagesRef = threadDoc.collection("messages")

                val batch = firestore.batch()
                var batchCount = 0

                for (msg in messages) {
                    val msgDoc = messagesRef.document(msg.id.toString())
                    val msgData = mapOf(
                        "body" to msg.body,
                        "date" to msg.timestamp,
                        "type" to (if (msg.outgoing) 2 else 1)
                    )
                    batch.set(msgDoc, msgData, SetOptions.merge())
                    batchCount++
                }
                if (batchCount > 0) {
                    batch.commit().await()
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
