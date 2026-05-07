## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2023-10-27 - Toggle Button Accessibility
**Learning:** State toggle buttons (like "Show/Hide Password") shouldn't change their `aria-label` dynamically. This confuses screen readers.
**Action:** Use a static `aria-label` (e.g., "Toggle password visibility") combined with the `aria-pressed` or `aria-expanded` attribute to indicate the current state.
