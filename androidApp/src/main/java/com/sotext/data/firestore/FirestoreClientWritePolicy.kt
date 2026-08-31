package com.sotext.data.firestore

/**
 * Field names on the `/users/{uid}` Firestore document that `firestore.rules`
 * treats as server-authoritative (see `isSubscriptionFieldChange()` there).
 * The server-side `verifySubscription`/`handlePlayStoreRTDN` Cloud Functions
 * own these; client writes touching one are rejected once its value actually
 * changes.
 *
 * Keep this in sync with the `protectedFields` list in `firestore.rules`.
 */
val PROTECTED_SUBSCRIPTION_FIELDS: Set<String> = setOf(
    "premiumSubscriptionStatus",
    "premiumSubscriptionExpiry",
    "tierBeforePremium",
    "subscriptionPurchaseToken",
    "subscriptionStatus",
    "premiumUnlocked",
    "proUnlocked",
    "hasPremiumHistory",
    "hasProHistory"
)

/**
 * Guards against the class of bug fixed on 2026-08-30 (see pulselink.txt): a
 * client-side `/users/{uid}` write that merges a [PROTECTED_SUBSCRIPTION_FIELDS]
 * key together with an unrelated, functionally-critical key (e.g.
 * `remoteWebAccessEnabled`). `firestore.rules` rejects such a write as a whole
 * whenever the protected value actually changes - which silently dropped
 * `remoteWebAccessEnabled` and aborted `SmsSyncWorker` before it ever synced a
 * single thread/message, for any user whose local subscription tier didn't
 * already match what was stored (e.g. every free-tier user, on their very
 * first sync).
 *
 * Call this on every payload before writing it to `/users/{uid}`. A pure,
 * protected-fields-only write (e.g. the best-effort tier mirror) or a pure
 * unprotected write (e.g. `remoteWebAccessEnabled` alone) is fine; mixing the
 * two in one write is what must never happen again.
 */
fun requireNoMixedProtectedWrite(payload: Map<String, Any?>) {
    val keys = payload.keys
    val touchesProtected = keys.any { it in PROTECTED_SUBSCRIPTION_FIELDS }
    val touchesUnprotected = keys.any { it !in PROTECTED_SUBSCRIPTION_FIELDS }
    check(!(touchesProtected && touchesUnprotected)) {
        "Firestore write to /users/{uid} mixes protected subscription field(s) with " +
            "unprotected field(s): $keys. firestore.rules rejects the whole write once a " +
            "protected value actually changes, which would silently drop the unprotected " +
            "fields too - split this into separate writes instead."
    }
}
