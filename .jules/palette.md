## 2025-05-15 - Interactive Containers
**Learning:** Users expect "input-like" containers (with icons and borders) to be fully interactive. Making the parent div focus the input on click reduces friction.
**Action:** Always add onClick handlers to custom input wrappers to forward focus to the inner input.
## 2026-04-17 - Make custom input wrappers focus the input field on click
**Learning:** Users expect custom input wrappers with borders and icons to focus the nested input when clicked anywhere inside the container.
**Action:** Always attach an onClick handler to the parent wrapper and use a ref to programmatically focus the underlying input field to expand the clickable area.
