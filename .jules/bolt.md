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
## 2024-05-04 - Replace array chains with imperative loops in React useMemo
**Learning:** Chained array methods (`.flat()`, `.map()`, `.filter()`) within heavily executed `useMemo` blocks can cause intermediate array allocations, memory bloat, and slower executions during React re-renders. Replacing them with single-pass imperative `for...of` loops avoids intermediate arrays, leading to better performance (~25% speedup in `combinedThreads` calculation).
**Action:** When working on computationally heavy hooks handling large arrays in React, use single-pass imperative `for...of` loops instead of chained declarative array methods. For `.flat()` replacements, handle elements explicitly via `Array.isArray(val) ? val : [val]` to avoid data loss on non-array elements.
