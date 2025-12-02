import Foundation
import Shared

@MainActor
final class AlertRelayViewModel: ObservableObject {
    @Published var statusText: String = "Idle"

    private let client: AlertRelayClient

    init(baseUrl: String = AlertRelay.shared.DEFAULT_BASE_URL) {
        client = AlertRelayFactory.shared.build(baseUrl: baseUrl)
    }

    func sendTestAlert() async {
        statusText = "Sending…"
        do {
            let recipient = AlertRecipient(
                phoneNumber: nil,
                pushToken: nil,
                email: nil
            )
            let request = AlertRequest(
                message: "Test alert from iOS shell",
                severity: AlertSeverity.emergency,
                recipients: [recipient],
                senderId: nil,
                location: nil,
                metadata: [:]
            )

            let response = try await client.sendAlert(request: request)
            statusText = "Sent (\(response.status)) id=\(response.relayId ?? "n/a")"
        } catch {
            statusText = "Failed: \(error.localizedDescription)"
        }
    }
}
