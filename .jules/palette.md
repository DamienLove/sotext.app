# Palette's Journal - Critical UX/A11y Learnings

## 2024-12-19 - Android SDK Constraints
**Learning:** The environment lacks Android SDK, so I cannot run UI tests or previews.
**Action:** Relied on code analysis and standard Compose patterns. Verification must be done by careful code review and ensuring valid Kotlin syntax.

## 2024-12-19 - Empty State Patterns
**Learning:** Empty states in Lists often default to simple text, which is easily missed.
**Action:** Implementing a centered, visual empty state (Icon + Text) using `fillParentMaxSize()` in LazyColumn items improves discoverability and polish.
