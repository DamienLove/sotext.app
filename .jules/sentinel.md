# Sentinel Journal

## 2025-12-17 - HTTP Wrapper Authentication Bypass
**Vulnerability:** The `alertRelayHttp` function, intended as a wrapper for clients without the Firebase SDK, failed to enforce authentication and allowed sender ID spoofing. It treated invalid tokens as anonymous users but then allowed anonymous users to send alerts.
**Learning:** When creating HTTP wrappers for Callable Cloud Functions, one must manually replicate the authentication and context checks that Callable functions provide automatically.
**Prevention:** Always verify `authUid` is present after decoding the token. Do not fall back to unauthenticated execution unless explicitly intended. Use the authenticated UID for sensitive fields like `senderId`.

## 2025-12-18 - IDOR in Link Channels
**Vulnerability:** The `linkChannels` collection and its `messages` subcollection allowed any authenticated user to read and write messages. Because channel IDs were derived from device IDs (which were also readable), an attacker could potentially enumerate and read private messages between users.
**Learning:** Firestore rules that rely on simple `auth != null` checks are insufficient for private user-to-user data, especially when document IDs are predictable or discoverable. Indirect ownership (User -> Device -> Message) requires helper functions and cross-document lookups (get checks).
**Prevention:** Implement strict ownership checks using `request.auth.uid`. If ownership is indirect (e.g., via a Device ID), use a helper function to validate the link between the user and the intermediate entity. Validate `senderId` on creation and `receiverId`/`senderId` on access.
