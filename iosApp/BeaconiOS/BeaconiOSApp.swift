import SwiftUI
import Shared
#if canImport(FirebaseCore)
import FirebaseCore
#endif

@main
struct BeaconiOSApp: App {
    @StateObject private var viewModel = BeaconViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: viewModel)
                .task {
                    // Configure Firebase if GoogleService-Info.plist is present
                    _ = FirebaseBootstrap.isConfigured
                }
        }
    }
}
