package com.sotext.data.firestore

import org.junit.Test

/**
 * Regression coverage for the 2026-08-30 web-access sync bug (see pulselink.txt):
 * SmsSyncWorker and MainViewModel used to merge `remoteWebAccessEnabled` into the
 * same Firestore write as protected subscription fields (e.g. `subscriptionStatus`).
 * `firestore.rules` rejects that write as a whole once the protected value actually
 * changes - which silently dropped `remoteWebAccessEnabled` and aborted the sync
 * worker before it ever synced a thread or message.
 */
class FirestoreClientWritePolicyTest {

    @Test
    fun `remoteWebAccessEnabled alone is allowed`() {
        requireNoMixedProtectedWrite(mapOf("remoteWebAccessEnabled" to true))
    }

    @Test
    fun `subscriptionStatus alone is allowed`() {
        requireNoMixedProtectedWrite(mapOf("subscriptionStatus" to "premium"))
    }

    @Test
    fun `multiple protected fields together are allowed`() {
        requireNoMixedProtectedWrite(
            mapOf(
                "subscriptionStatus" to "pro",
                "premiumUnlocked" to false,
                "proUnlocked" to true
            )
        )
    }

    @Test
    fun `multiple unprotected fields together are allowed`() {
        requireNoMixedProtectedWrite(
            mapOf(
                "remoteWebAccessEnabled" to true,
                "otpCleanupEnabled" to false,
                "timeFormat" to "H24"
            )
        )
    }

    @Test
    fun `empty payload is allowed`() {
        requireNoMixedProtectedWrite(emptyMap())
    }

    @Test(expected = IllegalStateException::class)
    fun `remoteWebAccessEnabled mixed with subscriptionStatus is rejected`() {
        requireNoMixedProtectedWrite(
            mapOf(
                "subscriptionStatus" to "premium",
                "remoteWebAccessEnabled" to true
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `remoteWebAccessEnabled mixed with premiumUnlocked and proUnlocked is rejected`() {
        // The exact original MainViewModel payload that caused the bug.
        requireNoMixedProtectedWrite(
            mapOf(
                "premiumUnlocked" to true,
                "proUnlocked" to false,
                "subscriptionStatus" to "premium",
                "remoteWebAccessEnabled" to true
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `a single unprotected field mixed with a single protected field is rejected`() {
        requireNoMixedProtectedWrite(
            mapOf(
                "hasPremiumHistory" to true,
                "otpCleanupEnabled" to true
            )
        )
    }
}
