# PulseLink iOS shell

This folder holds the starter SwiftUI app that consumes the Kotlin Multiplatform `Shared` framework for alert relay. Build steps (on macOS):

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
3. In Xcode, set your signing team, then build/run the `PulseLinkiOS` scheme.

Notes:
- The app imports `Shared` and uses `AlertRelayFactory.shared.build()` to send alerts to the backend relay.
- Update `AlertRelayFactory` base URL if you point to a non-production functions instance.
- Push/deep link handlers are stubbed; wire to APNs and universal links as you add entitlements.
