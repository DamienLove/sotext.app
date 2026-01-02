import SwiftUI
#if canImport(FirebaseMessaging)
import FirebaseMessaging

class MessagingDelegateAdapter: NSObject, MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Firebase registration token refreshed: \(fcmToken ?? "")")
        Task { @MainActor in
            await DeviceManager.shared.registerDevice()
        }
    }
}
#else
class MessagingDelegateAdapter: NSObject {}
#endif
