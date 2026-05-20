## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2026-05-20 - Toggle Button Accessibility
**Learning:** Using a static aria-label combined with aria-pressed provides better context to screen readers than dynamically changing the label for state toggle buttons.
**Action:** Always use aria-pressed for toggle buttons and stop propagation when inside interactive wrappers.
