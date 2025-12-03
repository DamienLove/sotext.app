import Foundation

#if canImport(FirebaseCore)
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
#endif

enum FirebaseBootstrap {
    static var isConfigured: Bool = {
        #if canImport(FirebaseCore)
        if FirebaseApp.app() == nil,
           let filePath = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           FileManager.default.fileExists(atPath: filePath) {
            FirebaseApp.configure()
        }
        return FirebaseApp.app() != nil
        #else
        return false
        #endif
    }()

    static func ensureAnonymousAuth() {
        #if canImport(FirebaseAuth)
        guard isConfigured else { return }
        if Auth.auth().currentUser == nil {
            Auth.auth().signInAnonymously { _, _ in }
        }
        #endif
    }
}
