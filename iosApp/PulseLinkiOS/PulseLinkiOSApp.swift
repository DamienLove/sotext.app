import SwiftUI
import Shared

@main
struct PulseLinkiOSApp: App {
    @StateObject private var relay = AlertRelayViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: relay)
        }
    }
}
