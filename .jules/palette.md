## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2025-05-15 - Input Wrapper Accessibility
**Learning:** When making static input wrappers interactive (e.g., adding onClick to forward focus), screen readers might incorrectly announce them as interactive elements unless marked with role="presentation". Additionally, inner interactive elements (like toggle buttons) need e.stopPropagation() to prevent unintended side effects.
**Action:** Always add role="presentation" to static wrapper elements when adding focus-forwarding onClick handlers, and use e.stopPropagation() on inner interactive elements.
