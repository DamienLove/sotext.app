1. **Optimize search filtering useMemo hooks**
   - Replace chained `.filter().map()` calls with single-pass `for...of` imperative loops in `filteredThreads`, `filteredThemes`, and `filteredDeviceContacts` in `web/src/App.jsx` to eliminate intermediate array allocations during frequent keystroke state changes.
2. **Optimize combinedThreads useMemo hook**
   - Replace the chained `.flat()`, `.map()`, `.filter()` and spread array allocations with an imperative loop in the `combinedThreads` hook in `web/src/App.jsx`.
   - Explicitly handle non-array elements when replacing `.flat()` to prevent data loss.
3. **Record Performance Learning**
   - Append the learning regarding imperative loops preventing array allocations in computationally heavy/frequent `useMemo` hooks to `.jules/bolt.md`.
4. **Verify Frontend Changes**
   - Run `cd web && pnpm lint` to ensure no linting regressions.
   - Run the local dev server and `cd web && npx playwright test` to verify no functionality is broken by the optimizations.
5. **Pre-commit Steps**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
6. **Submit PR**
   - Commit the changes and submit the PR on a new branch with a descriptive title format (`⚡ Bolt: [performance improvement]`).
