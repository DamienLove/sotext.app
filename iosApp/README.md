# SoText iOS (PulseLink / Beacon)

This folder contains the iOS counterpart to the Android SoText app. The Android app has been rebranded from PulseLink/Beacon to SoText (see the [root README](../README.md)); the iOS project has **not** been renamed yet — targets, bundle IDs, the workspace, and most in-app strings still say PulseLink/Beacon. Functionality is being kept in parity with Android under the old names pending an iOS rebrand pass.

## Apps & Variants

The project defines four build targets:
- **PulseLink Free** (`com.pulselink.ios.free`)
- **PulseLink Pro** (`com.pulselink.ios.pro`)
- **Beacon Free** (`com.pulselink.beacon.ios.free`)
- **Beacon Pro** (`com.pulselink.beacon.ios.pro`)

## Setup

1. Generate the CocoaPods spec and framework from the KMP module:
   ```bash
   ./gradlew :shared:podspec
   ```
2. Install pods and open the workspace:
   ```bash
   cd iosApp
   pod install
   open PulseLinkiOS.xcworkspace
   ```
3. In Xcode, ensure you select the appropriate scheme (e.g. `PulseLinkPro`).

## Functionality

- **Shared Logic**: Uses the `Shared` Kotlin Multiplatform module for core logic.
- **Communication**: Communicates with Android devices via Firebase Firestore (`outbox` and `lines` collections).
- **Pro Features**: The `PRO` compilation condition is used to enable features like Contacts (PulseLink) and Private Safe (Beacon).
- **Free/Pro parity with Android**: PulseLink Free now enables the Relay, Override, and Activity cards that used to be Pro-only, matching the Android free tier's "Emergency Button" utility model; the messaging Contacts tab remains Pro-gated.
- **Theming**: `RelayColors` in both apps track the "Future Deep v10" palette (Pitch Black `#000000` / Laser Blue `#00F3FF`) to stay aligned with the Web and Android design system.

## Status
- The iOS build is functionally behind Android. Recently shipped Android features — Catch Me Up (AI inbox briefing), context-aware actionable cards, and configurable inbox swipe gestures — have not been ported here yet.
- RingerSong (progressive-ringtone add-on) is Android-only; there is no iOS counterpart in this repo.

## Notes
- Update `AlertRelayFactory` base URL if you point to a non-production functions instance.
