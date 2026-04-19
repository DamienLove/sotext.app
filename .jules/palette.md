## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2024-04-19 - Interactive Input Wrappers & Toggle Buttons
**Learning:** When making an input wrapper interactive (like `onClick` to focus an inner input), inner state toggle buttons need `e.stopPropagation()` to avoid unintended focus side-effects. Also, state toggle buttons are more accessible with a static `aria-label` paired with an `aria-pressed` state, rather than dynamically changing the `aria-label` which can confuse screen readers.
**Action:** Always add `e.stopPropagation()` to interactive inner elements of clickable wrappers. Use static `aria-label` + `aria-pressed`/`aria-expanded` for state toggles.
