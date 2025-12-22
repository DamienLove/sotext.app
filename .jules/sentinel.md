# Sentinel Journal

## 2025-12-17 - HTTP Wrapper Authentication Bypass
**Vulnerability:** The `alertRelayHttp` function, intended as a wrapper for clients without the Firebase SDK, failed to enforce authentication and allowed sender ID spoofing. It treated invalid tokens as anonymous users but then allowed anonymous users to send alerts.
**Learning:** When creating HTTP wrappers for Callable Cloud Functions, one must manually replicate the authentication and context checks that Callable functions provide automatically.
**Prevention:** Always verify `authUid` is present after decoding the token. Do not fall back to unauthenticated execution unless explicitly intended. Use the authenticated UID for sensitive fields like `senderId`.

## 2025-12-18 - IDOR in Link Channels
**Vulnerability:** The `linkChannels` collection and its `messages` subcollection allowed any authenticated user to read and write messages. Because channel IDs were derived from device IDs (which were also readable), an attacker could potentially enumerate and read private messages between users.
**Learning:** Firestore rules that rely on simple `auth != null` checks are insufficient for private user-to-user data, especially when document IDs are predictable or discoverable. Indirect ownership (User -> Device -> Message) requires helper functions and cross-document lookups (get checks).
**Prevention:** Implement strict ownership checks using `request.auth.uid`. If ownership is indirect (e.g., via a Device ID), use a helper function to validate the link between the user and the intermediate entity. Validate `senderId` on creation and `receiverId`/`senderId` on access.

## 2025-12-19 - HTML Injection in Email Notifications
**Vulnerability:** The `sendEmailNotification` function interpolated raw user input (`senderName`, `body`) directly into HTML email templates, allowing attackers to inject malicious scripts or phishing links.
**Learning:** Email clients vary in sanitization, but generating HTML with untrusted string concatenation is fundamentally insecure. Always use an escaping function or a template engine.
**Prevention:** Implemented a lightweight `escapeHtml` helper to sanitize all user-provided strings before inserting them into HTML templates. Also URL-encoded parameters in generated links.

## 2025-12-21 - Device ID and Link Enumeration Fix
**Vulnerability:** The `devices` collection was globally readable, allowing harvesting of User ID to Device ID mappings. The `links` collection allowed listing all rendezvous points. `linkChannels` allowed modification of the parent document.
**Learning:** Firestore `allow read` implies `allow list`. Restricted collections must explicitly filter `list` operations using query constraints in the rules.
**Prevention:** Split `read` into `get` and `list`. Use `allow write: if false` for container documents. Explicitly validate ownership on `write` to prevent resource stealing.

## 2025-12-24 - Inconsistent Output Encoding
**Vulnerability:** Found `senderName` and `invitationCode` being interpolated into HTML email templates in `emailInvitations.ts` without sanitization, despite a similar fix existing in `email.ts`.
**Learning:** Security helpers (like `escapeHtml`) defined in one module (`email.ts`) are not automatically available or applied in others. Code duplication led to security regression/omission.
**Prevention:** Centralize security helpers in a shared `utils` module and mandate their usage in all HTML generation logic during code review.
