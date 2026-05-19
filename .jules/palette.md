## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2025-05-20 - Accessible Interactive Toggle Buttons
**Learning:** When using an interactive icon to toggle state (like a show/hide password button), dynamic aria-labels can confuse screen readers. Standard W3C ARIA practices prefer a static `aria-label` (e.g. 'Toggle password visibility') coupled with a dynamic `aria-pressed` attribute to clearly denote state changes without context shifting.
**Action:** Always use static `aria-label` combined with dynamic `aria-pressed` for UI toggle buttons rather than mutating the label text.
