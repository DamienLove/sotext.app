SoText v55 Tester Checklist (Free/Pro only)
Date: 2025-12-20

Scope
- Test Free and Pro only. Do not test Premium (testers are not briefed on Premium yet).
- Focus on the v55 release notes items.

Builds
- Free: `disposable_aabs/sotext-free-v55.aab`
- Pro: `disposable_aabs/sotext-pro-v55.aab`

Preflight
- Install v55 on a clean device.
- Upgrade from v54 to v55 (keep app data).
- Verify app version shows 55 in About/App info.

Feature Verification
1) SoText launcher icon branding
- Free: SoText icon matches Free branding.
- Pro: SoText icon matches Pro branding.
- Upgrade path: icon updates after v54 -> v55 without clearing data.
- Android 13+ themed icons: icon remains legible and correct.

2) Emergency + Check-in Glance widget actions
- Add Emergency and Check-in widgets to the home screen.
- Tap each widget action: verify it opens the correct Assistant shortcut flow.
- Cold start: force-stop app, then tap widget action.
- Warm start: open app once, return home, tap widget action.
- Reboot device: widgets persist and actions still route correctly.

3) Android 15 edge-to-edge compatibility (Material Components 1.13.0)
- Gesture navigation: verify content is not hidden behind system bars.
- 3-button navigation: verify no overlap at bottom bar.
- Display cutout/notch devices: verify top content is not clipped.
- Large text (accessibility): verify layouts remain readable.
- Rotate device: verify insets update correctly.

Negative / Break Tests
- Remove and re-add widgets repeatedly; actions still route correctly.
- Tap widget action while device is locked; app should open without crash.
- Toggle themed icons on/off; icon remains visible and correct.
- Switch navigation mode (gesture <-> 3-button) and re-check insets.

Reporting
- Note device model, Android version, and build flavor (Free/Pro).
- Attach screenshots or screen recordings for any failure.
