## 2024-05-24 - Firestore Identity Spoofing Fix
**Vulnerability:** The `users_by_phone` collection allowed any authenticated user to create a mapping for *any* phone number to their own UID, enabling identity spoofing.
**Learning:** Firestore rules do not automatically link `request.auth.token` attributes to document paths. You must explicitly enforce that the path variable (e.g., `phoneNumber`) matches the auth token claim (`request.auth.token.phone_number`).
**Prevention:** Always verify that user-claimable identifiers in document paths match the immutable claims in the auth token.

## 2024-05-25 - Shared Cache Poisoning
**Vulnerability:** The `callerIdCache` collection allowed any authenticated user to write to any document, enabling a malicious user to poison the global shared caller ID cache with false information (e.g., marking a safe number as spam).
**Learning:** Client-side "crowdsourcing" of shared data (like global caches) is inherently insecure if implemented via direct database writes (`allow write: if request.auth != null`). Malicious clients can always bypass client-side logic.
**Prevention:** Shared/global data structures must be read-only for clients. Writes must be mediated by a trusted backend (Cloud Function) that validates the source and content, or the feature must be redesigned to be user-specific (personal cache).
