# Sentinel Journal

## 2025-12-17 - HTTP Wrapper Authentication Bypass
**Vulnerability:** The `alertRelayHttp` function, intended as a wrapper for clients without the Firebase SDK, failed to enforce authentication and allowed sender ID spoofing. It treated invalid tokens as anonymous users but then allowed anonymous users to send alerts.
**Learning:** When creating HTTP wrappers for Callable Cloud Functions, one must manually replicate the authentication and context checks that Callable functions provide automatically.
**Prevention:** Always verify `authUid` is present after decoding the token. Do not fall back to unauthenticated execution unless explicitly intended. Use the authenticated UID for sensitive fields like `senderId`.
