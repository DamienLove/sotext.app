## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2025-05-18 - Clickable Password Wrapper
**Learning:** Users expect custom wrappers around inputs to pass focus down. If a password wrapper contains a toggle button, clicking the wrapper should focus the input.
**Action:** Add onClick and role='presentation' to password input wrappers, using e.stopPropagation on children buttons to prevent double-firing.
