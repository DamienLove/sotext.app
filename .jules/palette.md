## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2024-04-08 - Accessible state toggle buttons and click-to-focus wrappers
**Learning:** State toggle buttons (e.g. show/hide password) should use a static `aria-label` (like "Toggle password visibility") and dynamic `aria-pressed` or `aria-expanded` attributes, instead of mutating the `aria-label`. Additionally, input wrappers styled to look like larger inputs should forward clicks to focus the inner input, as users expect the entire visual field to be interactive.
**Action:** Always verify state buttons use `aria-pressed` instead of changing their label, and ensure custom input wrappers have an `onClick` that forwards focus via `useRef`.
