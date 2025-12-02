# PulseLink iOS Roadmap

Status: Planning/Scoping (Q4 2025)

PulseLink for iOS will bring the same discreet trigger and escalation flow to Apple devices. The initial release focuses on a reliable alert pipeline, privacy, and accessibility, with a Kotlin Multiplatform shared core where practical.

## Goals

- SwiftUI-first UX with VoiceOver-friendly flows
- On-device phrase detection for discreet triggers (evaluate Speech framework vs. wake-word libs)
- Alert escalation to trusted contacts with location context (user consent driven)
- Do Not Disturb-aware notifications using critical alerts where available
- Shared business logic via `shared/` KMP module where it makes sense

## Architecture Overview

- UI: SwiftUI + NavigationStack
- Alerts & Calls: CallKit, PushKit (for future server-initiated pings), UserNotifications
- Location: CoreLocation with background updates (sensitive; opt-in)
- Data: Keychain for secrets, AppStorage/UserDefaults for lightweight state
- Shared logic: Embed `Shared` KMP framework generated from `shared/` module

Build notes live in `iosApp/README.md`.

## Milestones

1) MVP (TestFlight private)
- KMP `Shared` integrated into iOS target
- Manual trigger (button/gesture) → alert dispatch to a test contact endpoint
- Location capture with explicit consent and clear indicators
- Basic settings screen; crash/analytics wired (privacy-respecting)

2) Beta (TestFlight public)
- Discreet phrase detection prototype (wake-word or short-phrase)
- Trusted contacts management (read-only import to start)
- Critical alert notifications and haptic/sound patterns
- SOS silent mode and quick cancel

3) GA 1.0 (App Store)
- Production-ready phrase detection with battery budget
- Contact write flows and secure export/import
- Background resiliency and edge-case handling
- App Store privacy labels and review assets

## Dependencies & Risks

- Phrase detection: iOS policies around background audio and always-on listeners require careful design to meet App Review guidelines
- Critical alerts entitlement: may require request/justification; fallback plans needed
- KMP interop: keep interfaces thin and Swift-friendly; avoid blocking the main thread

## How You Can Help

- iOS engineering (Swift/SwiftUI, CallKit/UserNotifications)
- Audio/ML expertise for low-latency wake-word or phrase detection
- QA on various device models and DnD configurations
- Accessibility testing (VoiceOver/large text/color contrast)

To contribute: open an issue describing scope, link to a small design note if possible, and propose a PR. See repository Issues for open items.

## Build & Run (quick)

See `iosApp/README.md` for full steps. Summary:

```bash
./gradlew :shared:podspec
cd iosApp
pod install
open PulseLinkiOS.xcworkspace
```

Set your signing team in Xcode, then run the `PulseLinkiOS` scheme.

## Funding Impact

Donations accelerate: Apple Developer Program fee, TestFlight device coverage, and wake-word R&D. See the main README’s Support section.
