## 2024-05-23 - React List Virtualization & Memoization
**Learning:** Large lists in React (like contact lists) that are rendered inline within a complex parent component (`App.jsx`) will strictly re-render every item whenever the parent re-renders, even if the list data hasn't changed.
**Action:** Extract list items into separate `memo`ized components. This allows React to skip re-rendering individual rows if their props (data) haven't changed, significantly reducing the main thread blocking time during interactions with other parts of the UI (like typing in inputs).

## 2025-02-23 - Double Rendering in Map & Gallery
**Learning:** I discovered that the Theme Gallery and Map Alert lists were being rendered TWICE. Once via the `ThemeGalleryItem`/`MapAlertItem` components, and then IMMEDIATELY AGAIN via an inline map block right next to it. This effectively doubled the DOM nodes for these lists and caused duplicate key warnings.
**Action:** Always check adjacent blocks of code when refactoring. It seems a previous refactor introduced the component but forgot to delete the old inline code.

## 2025-05-21 - Hoisting Callbacks for Dependencies
**Learning:** Callback functions intended for use in `useMemo` hooks (like `handleThreadSelect`) must be defined *textually before* the hook in the file. If defined after, they are inaccessible, forcing developers to bypass them and miss critical side-effects (like clearing message state to prevent ghost content).
**Action:** When organizing large components, ensure all handler functions are defined before the `useMemo` blocks that might need to reference them.
