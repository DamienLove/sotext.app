## 2024-05-23 - Firestore Open Writes
**Vulnerability:** The `public_profiles` collection allowed any authenticated user to write *any* data to their own profile document, and `users_by_phone` allowed enumeration.
**Learning:** `allow write` in Firestore defaults to allowing *any* schema. You must explicitly validate `request.resource.data.keys()` to prevent field injection (e.g. `isAdmin`, `isVerified`) or data dumping. Also `allow read` implies `list`, which causes enumeration risks.
**Prevention:** Use `keys().hasOnly([...])` for writes and `allow get` instead of `allow read` for lookups.

## 2024-05-23 - Firestore Delete vs Write
**Vulnerability:** A previous fix for `public_profiles` used `allow write` with `request.resource.data` validation. This broke `delete` operations because `request.resource` is null during deletion.
**Learning:** Always separate `create, update` rules from `delete` rules when validating payload schema.
**Prevention:** Use `allow create, update: if ... && request.resource.data...` and separate `allow delete`.
