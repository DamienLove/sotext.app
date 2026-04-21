## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2026-04-21 - Focus Forwarding Pattern
**Learning:** When making static wrapper elements interactive to forward focus to inner inputs, adding `role="presentation"` and using `useRef` instead of `getElementById` creates a robust and accessible UX enhancement without triggering strict linting rules like `jsx-a11y/no-static-element-interactions`.
**Action:** Always use `useRef` for DOM access in React, add `role="presentation"` to interactive wrappers, and ensure inner interactive elements use `e.stopPropagation()`.

## 2026-04-21 - Accessible Toggle Buttons
**Learning:** State toggle buttons (like 'Show/Hide Password') should use a static `aria-label` (e.g., 'Toggle password visibility') combined with the `aria-pressed` or `aria-expanded` attribute, rather than dynamically changing the `aria-label` based on state. This provides better context to screen readers.
**Action:** Use static ARIA labels and dynamic state attributes (`aria-pressed`, `aria-expanded`) for toggle controls.
