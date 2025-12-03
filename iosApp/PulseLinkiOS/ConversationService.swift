import Foundation

#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
#endif

protocol ConversationProvider {
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]]
    func send(message: ConversationMessage, to contact: ContactCard) async throws
}

final class InMemoryConversationProvider: ConversationProvider {
    private var store: [ContactCard: [ConversationMessage]] = [:]

    init(seed: [ContactCard: [ConversationMessage]] = [:]) {
        store = seed
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        store
    }

    func send(message: ConversationMessage, to contact: ContactCard) async throws {
        var arr = store[contact] ?? []
        arr.append(message)
        store[contact] = arr
    }
}

final class FirestoreConversationProvider: ConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    init(userId: String) {
        self.userId = userId
    }

    private var collection: CollectionReference {
        db.collection("relayThreads").document(userId).collection("messages")
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        let snapshot = try await collection.order(by: "timestamp").getDocuments()
        var result: [ContactCard: [ConversationMessage]] = [:]
        for doc in snapshot.documents {
            let data = doc.data()
            guard let contactName = data["contactName"] as? String,
                  let role = data["role"] as? String,
                  let presenceRaw = data["presence"] as? String,
                  let text = data["text"] as? String,
                  let ts = data["timestamp"] as? Timestamp else { continue }

            let presence: Presence = Presence(rawValue: presenceRaw) ?? .offline
            let contact = ContactCard(name: contactName, role: role, presence: presence, unread: 0)
            let msg = ConversationMessage(
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

    func send(message: ConversationMessage, to contact: ContactCard) async throws {
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
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] { [:] }
    func send(message: ConversationMessage, to contact: ContactCard) async throws {}
    #endif
}
