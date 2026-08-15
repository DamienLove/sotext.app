package com.sotext.data.beta

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sotext.BuildConfig
import com.sotext.auth.FirebaseAuthManager
import com.sotext.domain.repository.BetaAgreementRepository
import com.sotext.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class BetaAgreementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val settingsRepository: SettingsRepository,
    private val authManager: FirebaseAuthManager
) : BetaAgreementRepository {

    override suspend fun recordAgreement(name: String, agreementVersion: String) {
        authManager.ensureSignedIn()
        val deviceId = settingsRepository.ensureDeviceId()
        val payload = hashMapOf(
            "deviceId" to deviceId,
            "name" to name,
            "agreementVersion" to agreementVersion,
            "timestamp" to FieldValue.serverTimestamp(),
            "appVersionCode" to BuildConfig.VERSION_CODE,
            "appVersionName" to BuildConfig.VERSION_NAME
        )
        firestore.collection(COLLECTION)
            .document(deviceId)
            .set(payload, SetOptions.merge())
            .await()
    }

    companion object {
        private const val COLLECTION = "betaAgreements"
    }
}
