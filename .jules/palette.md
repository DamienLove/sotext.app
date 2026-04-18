## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2026-04-18 - State Toggle Button Accessibility
**Learning:** Dynamically changing the `aria-label` based on state (e.g. "Show password" to "Hide password") can confuse screen readers.
**Action:** Use a static `aria-label` (e.g., "Toggle password visibility") and indicate the active toggled state using the `aria-pressed` or `aria-expanded` boolean attribute to properly communicate state changes to screen readers.
