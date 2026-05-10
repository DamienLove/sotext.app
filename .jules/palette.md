## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2026-05-10 - State Toggle Buttons
**Learning:** For state toggle buttons (e.g., 'Show/Hide Password'), using a static aria-label combined with the aria-pressed attribute is more accessible than dynamically changing the aria-label text.
**Action:** Always use a static aria-label combined with aria-pressed to indicate the current state to screen readers.
