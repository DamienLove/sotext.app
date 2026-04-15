## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2026-04-15 - State Toggle Buttons
**Learning:** Dynamic aria-labels on toggle buttons cause confusion for screen reader users as they constantly change. Static labels combined with state attributes convey the action and current state accurately.
**Action:** Use a static `aria-label` alongside `aria-pressed` or `aria-expanded` for toggle buttons to indicate their active state.
