## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2025-05-15 - Password Visibility Toggle Accessibility
**Learning:** State toggle buttons (like "Show/Hide Password") should use a static `aria-label` combined with `aria-pressed` or `aria-expanded` to properly indicate their active toggled state to screen readers, rather than dynamically changing the `aria-label`.
**Action:** Use `aria-pressed` or `aria-expanded` attributes on toggle buttons along with a static `aria-label` that describes the purpose of the button.
