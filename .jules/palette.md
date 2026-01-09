## 2024-05-24 - List Item Loading States
**Learning:** Managing async actions (like deletion) in parent components for large lists causes expensive full-list re-renders and UI jank.
**Action:** Implement local `isLoading` / `isDeleting` state within `memo`-ized list item components to provide immediate, isolated feedback (spinner + disabled state) without re-rendering siblings.

## 2025-02-12 - Responsive Content Hiding vs. Accessibility
**Learning:** Hiding text labels with `display: none` on mobile/responsive views removes them from the accessibility tree, leaving icon-only buttons with no accessible name.
**Action:** Always ensure icon-only buttons (or buttons that become icon-only) have an explicit `aria-label` that matches the hidden text content.
