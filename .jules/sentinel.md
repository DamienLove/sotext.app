## 2024-05-24 - Firestore Identity Spoofing Fix
**Vulnerability:** The `users_by_phone` collection allowed any authenticated user to create a mapping for *any* phone number to their own UID, enabling identity spoofing.
**Learning:** Firestore rules do not automatically link `request.auth.token` attributes to document paths. You must explicitly enforce that the path variable (e.g., `phoneNumber`) matches the auth token claim (`request.auth.token.phone_number`).
**Prevention:** Always verify that user-claimable identifiers in document paths match the immutable claims in the auth token.
