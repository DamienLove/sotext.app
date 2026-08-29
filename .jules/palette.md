## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2025-05-15 - Accessible State Toggles
**Learning:** Screen readers and tests expect state toggle buttons (like "Show/Hide Password" or "Expand/Collapse") to use a static `aria-label` or `title` combined with `aria-pressed` or `aria-expanded` to properly indicate active state, rather than dynamically changing the `aria-label` text based on state.
**Action:** Always use static `aria-label` text with `aria-pressed` or `aria-expanded` attributes on toggle buttons instead of dynamic labels.
