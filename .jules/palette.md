## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2026-04-09 - Accessible State Toggle Buttons
**Learning:** Screen readers handle state changes more clearly when toggle buttons use a static `aria-label` combined with `aria-pressed` or `aria-expanded`, rather than dynamically changing the `aria-label` text based on state. This prevents confusion by clearly identifying the button and communicating its current active/inactive status separately.
**Action:** When implementing toggle buttons (e.g., show/hide password, collapse sidebar, expand details), always use a descriptive static `aria-label` along with dynamically updating the `aria-pressed` or `aria-expanded` attributes instead of swapping the label itself.
