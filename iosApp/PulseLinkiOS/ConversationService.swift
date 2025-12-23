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

    // Android Path: users/{uid}/synced_threads
    private var threadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        // 1. Fetch threads
        let threadsSnapshot = try await threadsCollection.getDocuments()

        // 2. Fetch messages in parallel using TaskGroup
        return try await withThrowingTaskGroup(of: (ContactCard, [ConversationMessage]).self) { group in
            for threadDoc in threadsSnapshot.documents {
                group.addTask {
                    let data = threadDoc.data()
                    let address = data["address"] as? String ?? "Unknown"
                    let contact = ContactCard(
                        name: address,
                        address: address,
                        role: "Contact",
                        presence: .offline,
                        unread: data["unread"] as? Int ?? 0
                    )

                    let messagesSnapshot = try await threadDoc.reference.collection("messages")
                        .order(by: "date", descending: false)
                        .limit(to: 50)
                        .getDocuments()

                    var messages: [ConversationMessage] = []
                    for msgDoc in messagesSnapshot.documents {
                        let msgData = msgDoc.data()
                        let type = msgData["type"] as? Int ?? 1 // 1=in, 2=out
                        let timestamp = msgData["date"] as? Int64 ?? 0
                        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)

                        let msg = ConversationMessage(
                            sender: type == 1 ? address : "You",
                            text: msgData["body"] as? String ?? "",
                            timestamp: date,
                            isIncoming: type == 1,
                            isUrgent: false
                        )
                        messages.append(msg)
                    }
                    return (contact, messages)
                }
            }

            var result: [ContactCard: [ConversationMessage]] = [:]
            for try await (contact, messages) in group {
                result[contact] = messages
            }
            return result
        }
    }

    func send(message: ConversationMessage, to contact: ContactCard) async throws {
        // Use contact.address (phone number) instead of contact.name (display name)
        let docData: [String: Any] = [
            "address": contact.address,
            "body": message.text,
            "date": Int64(message.timestamp.timeIntervalSince1970 * 1000),
            "sender": "iOS"
        ]

        try await db.collection("users").document(userId)
            .collection("outbox").addDocument(data: docData)
    }
    #else
    init(userId: String) {}
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] { [:] }
    func send(message: ConversationMessage, to contact: ContactCard) async throws {}
    #endif
}
