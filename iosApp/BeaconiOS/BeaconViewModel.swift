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

    @Published var isLoggedIn: Bool = false
    private var provider: BeaconConversationProvider
    private var threadsListener: ListenerRegistration?
    private var activeMessageListener: ListenerRegistration?
    private var currentListeningUserId: String?

    init() {
        // Initial provider setup
        provider = MockBeaconConversationProvider()

        setupAuthListener()
    }

    deinit {
        threadsListener?.remove()
        activeMessageListener?.remove()
    }

    private func setupAuthListener() {
        #if canImport(FirebaseAuth)
        if FirebaseBootstrap.isConfigured {
            Auth.auth().addStateDidChangeListener { [weak self] _, user in
                Task { @MainActor [weak self] in
                    guard let self = self else { return }
                    if let uid = user?.uid {
                        self.isLoggedIn = true
                        // Only update provider and restart listeners if user changed
                        if self.currentListeningUserId != uid {
                            self.currentListeningUserId = uid
                            self.provider = FirestoreBeaconConversationProvider(userId: uid)
                            self.startListeningToThreads()
                        }
                    } else {
                        self.isLoggedIn = false
                        self.currentListeningUserId = nil
                        self.threadsListener?.remove()
                        self.threadsListener = nil
                        self.activeMessageListener?.remove()
                        self.activeMessageListener = nil
                        self.provider = MockBeaconConversationProvider()
                        self.contacts = []
                        self.conversations = [:]
                        // Restart listening with mock provider to keep UI consistent
                        self.startListeningToThreads()
                    }
                }
            }
        }
        #else
        self.isLoggedIn = true
        startListeningToThreads()
        #endif
    }

    private func startListeningToThreads() {
        threadsListener?.remove()
        threadsListener = provider.listenToConversations { [weak self] newContacts in
            // Sort on background queue to avoid blocking UI
            let sorted = newContacts.sorted(by: { $0.unread > $1.unread })
            DispatchQueue.main.async {
                self?.contacts = sorted
            }
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
        // Validate message content
        let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedText.isEmpty else {
            statusText = "Cannot send empty message"
            return
        }

        // Limit message length (10,000 chars for safety)
        let maxLength = 10000
        let finalText = trimmedText.count > maxLength
            ? String(trimmedText.prefix(maxLength))
            : trimmedText

        if trimmedText.count > maxLength {
            print("⚠️ Message truncated from \(trimmedText.count) to \(maxLength) characters")
        }

        let message = BeaconConversationMessage(
            sender: "You",
            text: finalText,
            timestamp: Date(),
            isIncoming: false,
            isUrgent: false
        )

        Task {
            do {
                try await provider.send(message: message, to: contact)
                // Rely entirely on listener for UI updates (no optimistic update)
                // This prevents race conditions and duplicate messages
                await MainActor.run {
                    statusText = nil
                }
            } catch {
                await MainActor.run {
                    statusText = "Error sending: \(error.localizedDescription)"
                    print("Error sending message: \(error)")
                }
            }
        }
    }

    func startListeningToConversation(contact: BeaconContactCard) {
        activeMessageListener?.remove()
        activeMessageListener = provider.listenToMessages(for: contact) { [weak self] messages in
            DispatchQueue.main.async {
                self?.conversations[contact.id] = messages
            }
        }
    }

    func stopListeningToConversation() {
        activeMessageListener?.remove()
        activeMessageListener = nil
    }
}
