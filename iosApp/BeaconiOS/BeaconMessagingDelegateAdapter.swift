import SwiftUI
#if canImport(FirebaseMessaging)
import FirebaseMessaging

class BeaconMessagingDelegateAdapter: NSObject, MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Beacon Firebase registration token refreshed: \(fcmToken ?? "")")
        Task { @MainActor in
            await BeaconDeviceManager.shared.registerDevice()
        }
    }
}
#else
class BeaconMessagingDelegateAdapter: NSObject {}
#endif
