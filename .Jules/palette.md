# Palette's Journal

## 2024-10-18 - Search Input Patterns
**Learning:** Search fields on mobile require a clear button to avoid frustrating backspacing.
**Action:** Ensure all `OutlinedTextField` components used for search include a conditional trailing `Clear` icon.

## 2024-10-24 - Form Input Efficiency
**Learning:** Users abandon forms when they have to manually dismiss keyboards or switch modes for simple inputs like names/emails.
**Action:** Always enforce `singleLine = true`, proper `KeyboardType`, and `ImeAction.Next/Done` with associated actions for dialog inputs.

## 2025-12-18 - Password Visibility
**Learning:** Hidden password fields without a toggle increase user error rate and anxiety, especially on mobile devices where typing is prone to errors.
**Action:** Always include a "Show Password" toggle on password fields to improve accessibility and usability.

## 2025-05-18 - Empty State Visuals
**Learning:** Text-only empty states feel unfinished and lack prominence.
**Action:** Implement centered visual layouts with a decorative icon and descriptive text using `Modifier.fillParentMaxSize()` inside `LazyColumn` items.
