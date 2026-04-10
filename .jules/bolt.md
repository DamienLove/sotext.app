## 2024-05-22 - Linting Surprises
**Learning:** Found a duplicate `aria-label` prop in `web/src/App.jsx` that was breaking the lint check, even though it wasn't related to my changes.
**Action:** Always run `npm lint` locally before submitting, even if you think your changes are small. It catches pre-existing issues that might block the PR.

## 2024-05-24 - Eager useMemo Performance Traps
**Learning:** `useMemo` executes eagerly on every dependency change. For O(N) operations like search indexing (processing thousands of strings), this blocks the main thread on every render/update, even if the user isn't searching.
**Action:** Use lazy evaluation (like the `useLazySearchIndex` pattern) for expensive derivations that are only needed during specific interactions.

## 2024-05-27 - Array Allocation in Render Loops
**Learning:** `[...a, ...b].filter(Boolean).join(' • ')` is a common pattern for joining strings with separators, but it creates multiple intermediate arrays (spread, filter result) on every render.
**Action:** For simple string joining in hot paths (like list items), use imperative string concatenation or template literals to avoid unnecessary object allocation.

## 2024-05-28 - Date Allocation in Render Loops
**Learning:** `new Date()` allocation in hot render loops (like message lists) can be avoided by passing timestamps directly to `Intl.DateTimeFormat.format()`.
**Action:** Always check if formatting functions accept primitives before wrapping them in objects inside `render` or `map`.

## 2024-05-29 - Array Operations within useMemo
**Learning:** Chained array methods like `.flat().map().filter()` inside a frequently executed `useMemo` create substantial overhead by allocating intermediate arrays. This is especially true for data-heavy structures like a messaging inbox where rendering speed is critical.
**Action:** Replace functional array method chains with imperative single-pass loops inside React hooks that process lists, while preserving the logical flow.
