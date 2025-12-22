# Palette's Journal - Critical UX/A11y Learnings

## 2024-12-19 - Android SDK Constraints
**Learning:** The environment lacks Android SDK, so I cannot run UI tests or previews.
**Action:** Relied on code analysis and standard Compose patterns. Verification must be done by careful code review and ensuring valid Kotlin syntax.

## 2024-12-19 - Empty State Patterns
**Learning:** Empty states in Lists often default to simple text, which is easily missed.
**Action:** Implementing a centered, visual empty state (Icon + Text) using `fillParentMaxSize()` in LazyColumn items improves discoverability and polish.

## 2024-12-19 - Interactive Control Stability
**Learning:** Hiding primary actions (like a Send button) when inactive causes jarring layout shifts and reduces discoverability.
**Action:** Use disabled states with reduced opacity (`alpha = 0.38f` per Material spec) for controls that are temporarily invalid but relevant.

## 2024-12-19 - Expanded Touch Targets
**Learning:** In Settings screens, users expect the entire row to be interactive. Restricting interaction to a small Switch or Button decreases usability.
**Action:** Use `Modifier.toggleable` or `Surface(onClick=...)` on the container row to expand the touch target, while keeping the inner controls (Switch) visually present but passive.

## 2024-12-19 - Accessible List Items
**Learning:** Converting interactive `div` lists to `<button>` requires a robust CSS reset (border, bg, align, font) to maintain visual fidelity while gaining native keyboard support.
**Action:** Use a standardized CSS reset pattern for list items to ensure they are keyboard accessible without breaking the design.
