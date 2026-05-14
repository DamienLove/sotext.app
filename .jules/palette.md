## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2025-05-15 - Password Wrapper Focus
**Learning:** Adding an onClick handler to an input wrapper to forward focus improves UX by making the whole visual container interactive, but it requires stopping propagation on inner interactive elements (like the password visibility toggle) to prevent them from unintentionally stealing focus back or causing conflicting interactions.
**Action:** When making input wrappers interactive, always add `e.stopPropagation()` to the `onClick` handlers of any inner buttons, and pair `aria-pressed` with a static `aria-label` for standard toggle button accessibility.
