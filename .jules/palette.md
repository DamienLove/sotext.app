## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2024-04-12 - Ensure existing imports when adding EmptyState
**Learning:** When generating components like EmptyState or SearchIcon, verify they are imported and exist in the file scope to avoid application crashes.
**Action:** Use grep to check for `<SearchIcon` and `<EmptyState` before adding instances of them to components.
