import Foundation
#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
import UIKit
#endif

final class DeviceManager {
    static let shared = DeviceManager()

    private init() {}

    @MainActor
    func registerDevice() async {
        #if canImport(FirebaseFirestore)
        guard let user = Auth.auth().currentUser else { return }
        guard let deviceId = UIDevice.current.identifierForVendor?.uuidString else { return }

        let db = Firestore.firestore()
        let deviceData: [String: Any] = [
            "uid": user.uid,
            "name": UIDevice.current.name,
            "model": UIDevice.current.model,
            "platform": "iOS",
            "app": "PulseLink",
            "lastSeen": FieldValue.serverTimestamp()
        ]

        do {
            try await db.collection("devices").document(deviceId).setData(deviceData, merge: true)
            print("Device registered: \(deviceId)")
        } catch {
            print("Failed to register device: \(error.localizedDescription)")
        }
        #endif
    }
}
