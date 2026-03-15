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

## 2024-10-25 - Array Allocations in useMemo Hooks
**Learning:** Using functional array methods like `.flat()`, `.map()`, `.filter(Boolean)`, and spread operators `[...]` inside `useMemo` hooks for large datasets (like `threads` or `contacts`) creates significant intermediate garbage. For thousands of contacts or threads, this spikes GC pauses and blocks the main thread during the render phase.
**Action:** In high-volume data processing within `useMemo`, replace functional chains with imperative `for` loops and direct array pushes/Sets to prevent unnecessary object allocation and garbage collection.
