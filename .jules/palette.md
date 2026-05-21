## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2025-05-16 - Password Toggle Accessibility
**Learning:** Screen readers expect state toggle buttons to use aria-pressed with a static aria-label, rather than dynamically changing the text.
**Action:** Always use a static aria-label with aria-pressed for boolean toggle buttons.
