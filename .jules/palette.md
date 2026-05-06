## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2026-05-06 - State Toggle Button Accessibility
**Learning:** State toggle buttons (like 'Show/Hide Password') should use a static `aria-label` (e.g., 'Toggle password visibility') and rely on `aria-pressed` or `aria-expanded` to convey their current state to screen readers. Dynamically changing the label based on state can be confusing.
**Action:** Update the password toggle button to use a static label and `aria-pressed`.
