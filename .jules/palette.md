## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2026-05-13 - Inner Interactive Elements in Input Wrappers
**Learning:** When making an input wrapper interactive via an onClick handler to forward focus, inner interactive elements (like state toggle buttons) must use e.stopPropagation() to prevent unintended focus side effects.
**Action:** Always use e.stopPropagation() in onClick handlers of inner interactive elements when the parent wrapper is designed to forward focus.
