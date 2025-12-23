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
        let c1 = BeaconContactCard(name: "Alex Rivera", address: "5551234567", role: "Friend", presence: .online, unread: 2, isFavorite: true, isPrivate: false, isTrusted: false)
        let c2 = BeaconContactCard(name: "Morgan Lee", address: "5559876543", role: "Family", presence: .recent, unread: 0, isFavorite: false, isPrivate: true, isTrusted: true)

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

    private var threadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] {
        let threadsSnapshot = try await threadsCollection.getDocuments()

        return try await withThrowingTaskGroup(of: (BeaconContactCard, [BeaconConversationMessage]).self) { group in
            for threadDoc in threadsSnapshot.documents {
                group.addTask {
                    let data = threadDoc.data()
                    let address = data["address"] as? String ?? "Unknown"
                    let contact = BeaconContactCard(
                        name: address,
                        address: address,
                        role: "Contact",
                        presence: .offline,
                        unread: data["unread"] as? Int ?? 0,
                        isFavorite: data["isFavorite"] as? Bool ?? false,
                        isPrivate: data["isPrivate"] as? Bool ?? false,
                        isTrusted: data["isTrusted"] as? Bool ?? false
                    )

                    let messagesSnapshot = try await threadDoc.reference.collection("messages")
                        .order(by: "date", descending: false)
                        .limit(to: 50)
                        .getDocuments()

                    var messages: [BeaconConversationMessage] = []
                    for msgDoc in messagesSnapshot.documents {
                        let msgData = msgDoc.data()
                        let type = msgData["type"] as? Int ?? 1 // 1=in, 2=out
                        let timestamp = msgData["date"] as? Int64 ?? 0
                        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)

                        let msg = BeaconConversationMessage(
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

            var result: [BeaconContactCard: [BeaconConversationMessage]] = [:]
            for try await (contact, messages) in group {
                result[contact] = messages
            }
            return result
        }
    }

    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {
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
    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] { [:] }
    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {}
    #endif
}
