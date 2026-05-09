## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2024-05-09 - Accessible Focus Forwarding and State Toggles
**Learning:** When making an input wrapper interactive via an `onClick` handler to forward focus, ensure that any inner interactive elements (like state toggle buttons) use `e.stopPropagation()` in their `onClick` handlers to prevent unintended focus side effects. Also, for state toggle buttons (e.g., 'Show/Hide Password'), using a static `aria-label` combined with `aria-pressed` or `aria-expanded` is preferred over dynamically changing the `aria-label` text for screen readers. Lastly, static wrapper elements made interactive with `onClick` need `role="presentation"` to satisfy JSX accessibility rules.
**Action:** Use `e.stopPropagation()` on inner buttons inside clickable wrappers. Use static `aria-label` and `aria-pressed` for toggles. Add `role="presentation"` to layout wrappers acting as click targets.
