import Foundation
import Shared
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

@MainActor
final class BeaconViewModel: ObservableObject {
    @Published var contacts: [BeaconContactCard] = []
    @Published private(set) var conversations: [UUID: [BeaconConversationMessage]] = [:]

    private let provider: BeaconConversationProvider

    init() {
        if FirebaseBootstrap.isConfigured {
            FirebaseBootstrap.ensureAnonymousAuth()
            #if canImport(FirebaseAuth)
            if let uid = Auth.auth().currentUser?.uid {
                provider = FirestoreBeaconConversationProvider(userId: uid)
            } else {
                provider = MockBeaconConversationProvider()
            }
            #else
            provider = MockBeaconConversationProvider()
            #endif
        } else {
            provider = MockBeaconConversationProvider()
        }

        Task { await loadData() }
    }

    private func loadData() async {
        do {
            let fetched = try await provider.loadConversations()
            await MainActor.run {
                contacts = fetched.keys.sorted(by: { $0.name < $1.name })
                for (contact, msgs) in fetched {
                    conversations[contact.id] = msgs
                }
            }
        } catch {
            print("Failed to load conversations: \(error)")
        }
    }

    func messages(for contact: BeaconContactCard) -> [BeaconConversationMessage] {
        conversations[contact.id] ?? []
    }

    func sendMessage(to contact: BeaconContactCard, text: String) {
        let message = BeaconConversationMessage(
            sender: "You",
            text: text,
            timestamp: Date(),
            isIncoming: false,
            isUrgent: false
        )
        // Optimistic update
        var msgs = conversations[contact.id] ?? []
        msgs.append(message)
        conversations[contact.id] = msgs

        Task {
            try? await provider.send(message: message, to: contact)
        }
    }
}
