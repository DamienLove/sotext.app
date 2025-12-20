import Foundation
#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
#endif

protocol BeaconConversationProvider {
    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]]
    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws
}

final class MockBeaconConversationProvider: BeaconConversationProvider {
    private var store: [BeaconContactCard: [BeaconConversationMessage]] = [:]

    init() {
        // Seed some data for when Firebase is not configured
        let c1 = BeaconContactCard(name: "Alex Rivera", role: "Friend", presence: .online, unread: 2)
        let c2 = BeaconContactCard(name: "Morgan Lee", role: "Family", presence: .recent, unread: 0)

        store[c1] = [
            BeaconConversationMessage(sender: "Alex", text: "Hey, how are you?", timestamp: Date().addingTimeInterval(-3600), isIncoming: true, isUrgent: false),
            BeaconConversationMessage(sender: "You", text: "I'm good!", timestamp: Date().addingTimeInterval(-3500), isIncoming: false, isUrgent: false)
        ]
        store[c2] = []
    }

    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] {
        store
    }

    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {
        var arr = store[contact] ?? []
        arr.append(message)
        store[contact] = arr
    }
}

final class FirestoreBeaconConversationProvider: BeaconConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    init(userId: String) {
        self.userId = userId
    }

    private var collection: CollectionReference {
        db.collection("beaconThreads").document(userId).collection("messages")
    }

    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] {
        let snapshot = try await collection.order(by: "timestamp").getDocuments()
        var result: [BeaconContactCard: [BeaconConversationMessage]] = [:]

        // Group by contact name for now since we don't have unique contact IDs in this simple model
        var contactMap: [String: BeaconContactCard] = [:]

        for doc in snapshot.documents {
            let data = doc.data()
            guard let contactName = data["contactName"] as? String,
                  let role = data["role"] as? String,
                  let presenceRaw = data["presence"] as? String,
                  let text = data["text"] as? String,
                  let ts = data["timestamp"] as? Timestamp else { continue }

            let presence = BeaconPresence(rawValue: presenceRaw) ?? .offline

            let contact: BeaconContactCard
            if let existing = contactMap[contactName] {
                contact = existing
            } else {
                contact = BeaconContactCard(name: contactName, role: role, presence: presence, unread: 0)
                contactMap[contactName] = contact
            }

            let msg = BeaconConversationMessage(
                sender: data["sender"] as? String ?? "Unknown",
                text: text,
                timestamp: ts.dateValue(),
                isIncoming: data["incoming"] as? Bool ?? false,
                isUrgent: data["urgent"] as? Bool ?? false
            )
            result[contact, default: []].append(msg)
        }
        return result
    }

    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {
        var presenceRaw = "offline"
        switch contact.presence {
        case .online: presenceRaw = "online"
        case .recent: presenceRaw = "recent"
        case .offline: presenceRaw = "offline"
        }
        try await collection.addDocument(data: [
            "contactName": contact.name,
            "role": contact.role,
            "presence": presenceRaw,
            "text": message.text,
            "sender": message.sender,
            "incoming": message.isIncoming,
            "urgent": message.isUrgent,
            "timestamp": Timestamp(date: message.timestamp)
        ])
    }
    #else
    init(userId: String) {}
    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] { [:] }
    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {}
    #endif
}
