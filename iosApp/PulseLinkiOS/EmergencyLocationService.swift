import Foundation
#if canImport(FirebaseFirestore)
import FirebaseFirestore
#endif
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

final class EmergencyLocationService {
    #if canImport(FirebaseFirestore) && canImport(FirebaseAuth)
    private let db = Firestore.firestore()

    func recordEmergencyLocation(message: String?) async -> Bool {
        guard FirebaseBootstrap.isConfigured else { return false }
        guard let uid = Auth.auth().currentUser?.uid else { return false }
        let location = await EmergencyLocationProvider.shared.requestLocation()
        guard let location else { return false }

        var payload: [String: Any] = [
            "lat": location.coordinate.latitude,
            "lng": location.coordinate.longitude,
            "severity": "emergency",
            "incoming": false,
            "createdAt": FieldValue.serverTimestamp(),
            "clearedAt": NSNull()
        ]
        if location.horizontalAccuracy > 0 {
            payload["accuracyMeters"] = location.horizontalAccuracy
        }
        if let name = Auth.auth().currentUser?.displayName, !name.isEmpty {
            payload["sourceName"] = name
        }
        if let message, !message.isEmpty {
            payload["message"] = message
        }

        do {
            _ = try await db.collection("users").document(uid)
                .collection("emergencyLocations")
                .addDocument(data: payload)
            return true
        } catch {
            return false
        }
    }
    #else
    func recordEmergencyLocation(message: String?) async -> Bool { false }
    #endif
}
