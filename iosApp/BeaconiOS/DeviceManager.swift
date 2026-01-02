import Foundation

#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
#endif

final class DeviceManager {
    static let shared = DeviceManager()

    private init() {}

    func registerDevice(userId: String) async {
        #if canImport(FirebaseFirestore)
        guard let deviceId = await getDeviceId() else { return }

        let db = Firestore.firestore()
        let docRef = db.collection("devices").document(deviceId)

        let data: [String: Any] = [
            "uid": userId,
            "app": "Beacon",
            "platform": "iOS",
            "updatedAt": FieldValue.serverTimestamp(),
            // fcmToken would go here if we had FirebaseMessaging
        ]

        do {
            try await docRef.setData(data, merge: true)
            print("Device registered: \(deviceId)")
        } catch {
            print("Error registering device: \(error)")
        }
        #endif
    }

    private func getDeviceId() async -> String? {
        // Use Identifier for Vendor as a stable device ID
        return UIDevice.current.identifierForVendor?.uuidString
    }
}
