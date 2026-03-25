## 2024-01-05 - AI Prompt Injection via Scalar Fields
**Vulnerability:** Indirect Prompt Injection (Instruction Hijacking) was possible via the `contactName` field in the AI summary flow. Although HTML-escaped, the field allowed newlines, enabling attackers to inject fake "System:" instructions or mock data blocks outside the intended context.
**Learning:** `escapeHtml` is insufficient for LLM security because it protects against XSS (browser interpretation) but not against structural manipulation of the prompt (LLM interpretation). Newlines are semantic delimiters in many prompt templates.
**Prevention:** Use `sanitizeScalar` to strip newlines and control characters from simple text fields before embedding them in prompts. Treat all user input as untrusted data, not just for HTML tags but for prompt structure.

## 2024-05-23 - [Insecure Default in Extension Approval]
**Vulnerability:** The `onExtensionSubmitted` function was configured to auto-approve all submitted extensions by default ("for now") in what was intended as a dev-only convenience, but without strict environment checks.
**Learning:** Temporary "dev-only" shortcuts often lack robust guards (like checking `FUNCTIONS_EMULATOR`) and can easily slip into production or become permanent features if not caught.
**Prevention:** Avoid "allow all" defaults even in development. Implement the actual security check (e.g., admin role) immediately, or use strict environment variable checks if a bypass is truly needed.

## 2024-05-24 - [Missing Security Headers]
**Vulnerability:** The web application hosting configuration (`firebase.json`) lacked standard security headers (HSTS, X-Frame-Options, X-Content-Type-Options), leaving the app vulnerable to Clickjacking, MIME sniffing, and SSL stripping.
**Learning:** Single Page Applications (SPAs) hosted on static CDNs (like Firebase Hosting) do not inherit security headers by default; they must be explicitly configured in the hosting config file.
**Prevention:** Always audit `firebase.json` (or equivalent) for a `headers` section. Enforce `X-Frame-Options: DENY` for main applications and `Strict-Transport-Security` for all production domains.
## 2024-05-24 - Firestore Data Enumeration Prevention
**Vulnerability:** The `callerIdCache` collection in `firestore.rules` used `allow read`, which implicitly grants `list` permission. This allowed any authenticated user to download the entire dataset of cached phone numbers and names, a potential privacy leak.
**Learning:** `allow read` is a shorthand for `get` and `list`. For collections containing user data or PII that are accessed via key-value lookups (like caches or user profiles), `allow get` is safer than `allow read`.
**Prevention:** Always verify if `list` permission is actually required. If the app only looks up documents by ID, restrict the rule to `allow get`.

## 2024-05-25 - AI Prompt Injection in Natural Language Interface
**Vulnerability:** The `naturalLanguageQueryFlow` Cloud Function embedded raw user input directly into the prompt template without sanitization, allowing potential instruction hijacking via newline injection.
**Learning:** Even simple query inputs can be vectors for prompt injection if they can alter the structure of the prompt (e.g., by simulating new system instructions).
**Prevention:** Always use `sanitizeScalar` (or equivalent) to strip control characters and newlines from user input before embedding it into LLM prompts.

## 2024-05-26 - [Bypassed Moderation via Direct Write]
**Vulnerability:** The frontend attempted to write "safe" content directly to a public, read-only collection (`themes_public`) to bypass moderation, which failed due to correct Firestore rules but highlighted a flaw in the design where client-side logic determined security posture.
**Learning:** Never rely on client-side logic ("it has no images") to bypass security queues. If the destination is protected, all writes must go through a privileged backend (Cloud Function) or a submission queue (`themes_submissions`).
**Prevention:** Implement the "Submission Queue" pattern: Clients always write to a pending collection. A Cloud Function trigger validates the content server-side and promotes it to the public collection if safe, or flags it for review.

## 2024-05-27 - [Persistent XSS via Direct Profile Updates]
**Vulnerability:** While theme submissions were validated for XSS vectors (like `javascript:` URLs) by a backend Cloud Function, user profile updates (avatar, theme preferences) were written directly to Firestore via `public_profiles` without content validation in `firestore.rules`.
**Learning:** Backend validation (Cloud Functions triggers) only protects data flowing through that specific pipeline (e.g., submission queues). It does not protect direct database writes allowed by security rules. Security must be enforced at the entry point (Firestore Rules) for direct writes.
**Prevention:** Implement validation functions (e.g., `isValidImageUrl`) directly in `firestore.rules` and enforce them on all fields that accept URLs or sensitive content in `create` and `update` operations.

## 2024-05-28 - [Initialization Issue with Firebase Secret Manager]
**Vulnerability:** Initializing resources (like `nodemailer.createTransport`) at the global scope using `defineSecret().value()` caused runtime crashes because secret values are not available during the module load phase in Firebase Functions.
**Learning:** When using `defineSecret()` from `firebase-functions/params`, the secret values are only resolved when the function is actually executed, not when the file is loaded. Global initialization with `.value()` will fail or result in undefined values.
**Prevention:** Always perform lazy initialization of resources that depend on secret values inside the function handler. For example, move the `nodemailer.createTransport` call inside the `onCall` or `onRequest` handler to ensure secrets are correctly populated.
