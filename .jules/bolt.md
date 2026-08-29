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

## 2026-03-11 - Array Allocation in Render Loops (Revisited)
**Learning:** Spread operators and `forEach` inside `useMemo` hooks (like `contactLookup` processing thousands of contacts) cause significant intermediate array allocations and garbage collection overhead, slowing down the main thread.
**Action:** Replace array spreads and `forEach` loops with standard `for` loops in hot paths or large data derivations.
## 2024-05-03 - Prevent array allocations in computationally heavy useMemo hooks
**Learning:** Chained array methods (like `.flat()`, `.map()`, `.filter()`) and spread operators create intermediate arrays on every evaluation. In frequently executing `useMemo` hooks processing large lists (e.g., search filtering on keystrokes or combining complex thread state), these intermediate allocations cause unnecessary memory bloat and degrade performance.
**Action:** Replace chained declarative array methods with single-pass imperative `for...of` loops within heavy `useMemo` blocks to perform the logic in a single pass without allocating temporary arrays. Always explicitly handle non-array elements when replacing `.flat()` to prevent data loss.
