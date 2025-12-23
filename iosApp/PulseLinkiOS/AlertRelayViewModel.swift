import Foundation
import Shared
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

enum Presence: String {
    case online, recent, offline

    var label: String {
        switch self {
        case .online: return "Online"
        case .recent: return "Recent"
        case .offline: return "Offline"
        }
    }
}

struct ContactCard: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let address: String // Phone number or unique ID
    let role: String
    let presence: Presence
    var unread: Int
}

struct ConversationMessage: Identifiable {
    let id = UUID()
    let sender: String
    let text: String
    let timestamp: Date
    let isIncoming: Bool
    let isUrgent: Bool
}

enum EmergencyState: Equatable {
    case idle
    case arming(Int)   // countdown seconds
    case active
}

@MainActor
final class AlertRelayViewModel: ObservableObject {
    @Published var statusText: String = "Idle"
    @Published var emergencyState: EmergencyState = .idle
    @Published var contacts: [ContactCard] = []
    @Published private(set) var conversations: [UUID: [ConversationMessage]] = [:]
    @Published var overrideDND: Bool = true
    @Published var maxVolumeOnUrgent: Bool = true
    @Published var baseUrl: String

    private let client: AlertRelayClient
    private let conversationProvider: ConversationProvider
    private var armingTask: Task<Void, Never>?
    private let pinCode = "1234" // demo PIN; replace with secure storage

    init(baseUrl: String = AlertRelay.shared.DEFAULT_BASE_URL) {
        self.baseUrl = baseUrl
        client = AlertRelayFactory.shared.build(baseUrl: baseUrl)
        if FirebaseBootstrap.isConfigured {
            FirebaseBootstrap.ensureAnonymousAuth()
            #if canImport(FirebaseAuth)
            if let uid = Auth.auth().currentUser?.uid {
                conversationProvider = FirestoreConversationProvider(userId: uid)
            } else {
                conversationProvider = InMemoryConversationProvider()
            }
            #else
            conversationProvider = InMemoryConversationProvider()
            #endif
        } else {
            conversationProvider = InMemoryConversationProvider()
        }

        Task { await loadConversations() }
    }

    // MARK: - Relay

    func sendTestAlert() async {
        statusText = "Sending…"
        do {
            let recipient = AlertRecipient(
                phoneNumber: nil,
                pushToken: nil,
                email: nil
            )
            let request = AlertRequest(
                message: "Test alert from iOS shell",
                severity: AlertSeverity.emergency,
                recipients: [recipient],
                senderId: nil,
                location: nil,
                metadata: [:]
            )

            let response = try await client.sendAlert(request: request)
            statusText = "Status: \(response.status) id=\(response.relayId ?? "n/a")"
        } catch {
            statusText = "Failed: \(error.localizedDescription)"
        }
    }

    // MARK: - Emergency flow

    func startEmergencyCountdown(seconds: Int = 5) {
        guard emergencyState == .idle else { return }
        emergencyState = .arming(seconds)
        armingTask?.cancel()
        armingTask = Task { [weak self] in
            var remaining = seconds
            while remaining > 0 && !(Task.isCancelled) {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                remaining -= 1
                await MainActor.run {
                    self?.emergencyState = .arming(remaining)
                }
            }
            guard !Task.isCancelled else { return }
            await MainActor.run {
                self?.emergencyState = .active
                self?.statusText = "Emergency active"
            }
        }
    }

    func cancelEmergency(withPin pin: String) -> Bool {
        guard emergencyState != .idle else { return true }
        guard pin == pinCode else { return false }
        armingTask?.cancel()
        emergencyState = .idle
        statusText = "Emergency canceled"
        return true
    }

    func cancelEmergencyBypassPin() {
        armingTask?.cancel()
        emergencyState = .idle
        statusText = "Emergency canceled"
    }

    // MARK: - Conversations

    func messages(for contact: ContactCard) -> [ConversationMessage] {
        conversations[contact.id] ?? []
    }

    func sendMessage(to contact: ContactCard, text: String, urgent: Bool) {
        let message = ConversationMessage(
            sender: "You",
            text: text,
            timestamp: Date(),
            isIncoming: false,
            isUrgent: urgent
        )
        Task {
            do {
                try await conversationProvider.send(message: message, to: contact)
                // Only update UI if send succeeded (which it won't for Firestore provider yet)
                await MainActor.run {
                    var msgs = conversations[contact.id] ?? []
                    msgs.append(message)
                    conversations[contact.id] = msgs
                    statusText = urgent ? "Urgent message sent (ringer boost, DND override)" : "Message sent"
                    if urgent {
                        NotificationManager.shared.playAlertToneIfNeeded(volumeBoost: maxVolumeOnUrgent)
                        NotificationManager.shared.postCriticalLocal(
                            title: "Urgent to \(contact.name)",
                            body: text,
                            volumeBoost: maxVolumeOnUrgent
                        )
                    }
                }
            } catch {
                await MainActor.run {
                    statusText = "Send failed: \(error.localizedDescription)"
                }
            }
        }
    }

    private func loadConversations() async {
        if let fetched = try? await conversationProvider.loadConversations() {
            await MainActor.run {
                var newConversations: [UUID: [ConversationMessage]] = [:]
                var newContacts: [ContactCard] = []

                for (contact, msgs) in fetched {
                    newContacts.append(contact)
                    newConversations[contact.id] = msgs
                }

                self.contacts = newContacts.sorted(by: { $0.unread > $1.unread })
                self.conversations = newConversations
            }
        }
    }
}
