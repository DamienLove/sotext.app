import SwiftUI
import Shared
#if canImport(FirebaseCore)
import FirebaseCore
#endif

@main
struct PulseLinkiOSApp: App {
    @StateObject private var relay = AlertRelayViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: relay)
                .task {
                    // Configure Firebase if GoogleService-Info.plist is present
                    _ = FirebaseBootstrap.isConfigured
                    // Ask for critical alert permission up front
                    _ = await NotificationManager.shared.requestCriticalAlertsPermission()
                }
        }
    }
}
