## 2024-05-24 - Improved Async Feedback
**Learning:** Users lack confidence in async operations (like saving profiles) when buttons don't provide immediate visual feedback (disabling/loading state).
**Action:** Always wrap async handlers in a `try/finally` block that toggles an `isProcessing` state, and use this state to disable buttons and update their labels (e.g., "Save" -> "Saving...").

## 2024-05-24 - Empty States for Lists
**Learning:** Empty lists (like zero threads) in sidebars can look like broken UI or loading errors to new users.
**Action:** Always provide a semantic empty state (even if simple text) for lists that can be legitimately empty, guiding the user on what to expect or do next.
