## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2024-05-24 - Password Toggle Button Accessibility
**Learning:** For interactive state toggle buttons (like "Show/Hide Password"), changing the `aria-label` dynamically makes it harder for screen reader users to track the button's purpose and state.
**Action:** Use a static descriptive `aria-label` (e.g., "Toggle password visibility") combined with `aria-pressed={state}` to correctly announce the button's active toggled state.
