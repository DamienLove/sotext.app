## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2025-04-03 - Static aria-label with aria-pressed for Toggle Buttons
**Learning:** When implementing accessibility for state toggle buttons (e.g., "Show/Hide Password" or "Show Inbox/Archive"), dynamically changing the `aria-label` based on state can be confusing to screen readers because it doesn't clearly indicate that it's a toggle button. It's better to use a static `aria-label` (e.g., "Toggle password visibility") combined with the `aria-pressed` attribute to properly indicate their active toggled state.
**Action:** Always use static `aria-label` and dynamic `aria-pressed` or `aria-expanded` attributes for toggle buttons to provide consistent and clear feedback to screen readers.
