## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2024-04-16 - State Toggle Button Accessibility
**Learning:** For state toggle buttons (e.g., 'Show/Hide Password'), dynamically changing the `aria-label` based on state can be confusing to screen readers as the label mutates upon interaction. It is better to use a static `aria-label` combined with `aria-pressed` or `aria-expanded` to represent the active state.
**Action:** Next time I implement or fix a toggle button, I will use a static `aria-label` (like "Toggle password visibility") and dynamically set `aria-pressed={isToggled}` instead of swapping the label string.
