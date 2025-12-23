import Foundation
import Shared
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

@MainActor
final class BeaconViewModel: ObservableObject {
    @Published var contacts: [BeaconContactCard] = []
    @Published private(set) var conversations: [UUID: [BeaconConversationMessage]] = [:]
    @Published var statusText: String? = nil

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
                contacts = fetched.keys.sorted(by: { $0.unread > $1.unread })
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

    // MARK: - Filters

    var inboxContacts: [BeaconContactCard] {
        contacts.filter { !$0.isPrivate }
    }

    var trustedContacts: [BeaconContactCard] {
        contacts.filter { $0.isTrusted && !$0.isPrivate }
    }

    var favoriteContacts: [BeaconContactCard] {
        contacts.filter { $0.isFavorite && !$0.isPrivate }
    }

    var privateContacts: [BeaconContactCard] {
        contacts.filter { $0.isPrivate }
    }

    func sendMessage(to contact: BeaconContactCard, text: String) {
        let message = BeaconConversationMessage(
            sender: "You",
            text: text,
            timestamp: Date(),
            isIncoming: false,
            isUrgent: false
        )

        Task {
            do {
                try await provider.send(message: message, to: contact)
                // Only optimistic update if send didn't throw
                await MainActor.run {
                    var msgs = conversations[contact.id] ?? []
                    msgs.append(message)
                    conversations[contact.id] = msgs
                    statusText = nil
                }
            } catch {
                await MainActor.run {
                    // For now we just log, or bind this to a UI alert if ContentView supports it
                    statusText = "Error sending: \(error.localizedDescription)"
                    print("Error sending message: \(error)")
                }
            }
        }
    }
}
