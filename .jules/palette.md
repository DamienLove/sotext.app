## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.

## 2025-05-16 - Static ARIA labels for toggles
**Learning:** Dynamic aria-labels on state toggles (e.g. 'Show' vs 'Hide') can confuse screen readers by changing their identity.
**Action:** Use a static aria-label (e.g., 'Toggle password visibility') combined with aria-pressed or aria-expanded to indicate active state.
