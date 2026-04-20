## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2024-05-24 - Interactive Containers Strict Accessibility
**Learning:** Adding an onClick handler to a static element (like a div wrapping an input) triggers strict accessibility linting rules (e.g. jsx-a11y/no-static-element-interactions) even if it functionally improves UX.
**Action:** Always add `role="presentation"` to static wrapper elements when adding focus-forwarding onClick handlers to satisfy strict a11y linters while preserving the UX enhancement.
