import SwiftUI

struct ContentView: View {
    @ObservedObject var viewModel: AlertRelayViewModel

    private let sampleContacts: [ContactChip] = [
        ContactChip(name: "Alex Rivera", role: "Emergency", status: .online),
        ContactChip(name: "Morgan Lee", role: "Check-in", status: .away),
        ContactChip(name: "Jamie Patel", role: "Backup", status: .offline)
    ]

    private let sampleActivity: [ActivityRow] = [
        ActivityRow(icon: "bolt.fill", title: "Emergency drill sent", detail: "Queued for 3 recipients"),
        ActivityRow(icon: "checkmark.shield.fill", title: "Check-in acknowledged", detail: "2m ago • Morgan"),
        ActivityRow(icon: "bubble.left.and.exclamationmark.fill", title: "Alert log synced", detail: "Today • Cloud")
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    heroCard
                    actionsCard
                    contactsCard
                    activityCard
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
            }
            .background(LinearGradient(colors: [.black, RelayColors.deep],
                                       startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea())
            .navigationTitle("PulseLink")
        }
    }

    // MARK: - Sections

    private var heroCard: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(LinearGradient(colors: [RelayColors.primary, RelayColors.accent],
                                     startPoint: .topLeading, endPoint: .bottomTrailing))
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "antenna.radiowaves.left.and.right")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.9))
                        .padding(10)
                        .background(.white.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("PulseLink Relay")
                            .font(.title2.bold())
                            .foregroundStyle(.white)
                        Text("Send practice alerts and monitor relay health.")
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.85))
                            .lineLimit(2)
                    }
                }

                statusPill(text: viewModel.statusText)
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
    }

    private var actionsCard: some View {
        Card {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Actions")
                        .font(.headline)
                    Text("Trigger a test alert to verify end-to-end delivery.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "paperplane.fill")
                    .foregroundStyle(RelayColors.primary)
            }
            Button {
                Task { await viewModel.sendTestAlert() }
            } label: {
                Label("Send Test Alert", systemImage: "bolt.horizontal.fill")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(RelayColors.primary)

            Divider()
            Label(viewModel.statusText, systemImage: "waveform.path.ecg")
                .foregroundStyle(.secondary)
        }
    }

    private var contactsCard: some View {
        Card {
            HStack {
                Text("Trusted contacts")
                    .font(.headline)
                Spacer()
                Text("\(sampleContacts.count) linked")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            VStack(spacing: 12) {
                ForEach(sampleContacts) { contact in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(contact.name)
                                .font(.body.weight(.semibold))
                            Text(contact.role)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        statusDot(for: contact.status)
                    }
                    .padding(.vertical, 6)
                }
            }
        }
    }

    private var activityCard: some View {
        Card {
            HStack {
                Text("Recent activity")
                    .font(.headline)
                Spacer()
                Image(systemName: "arrow.clockwise")
                    .foregroundStyle(.secondary)
            }
            VStack(spacing: 12) {
                ForEach(sampleActivity) { item in
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: item.icon)
                            .foregroundStyle(RelayColors.accent)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(.body.weight(.semibold))
                            Text(item.detail)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private func statusPill(text: String) -> some View {
        Text(text)
            .font(.callout.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(.white.opacity(0.15))
            .clipShape(Capsule())
    }

    private func statusDot(for status: Presence) -> some View {
        HStack(spacing: 6) {
            Circle()
                .fill(status.color)
                .frame(width: 10, height: 10)
            Text(status.label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color(.secondarySystemBackground))
        .clipShape(Capsule())
    }
}

// MARK: - UI primitives

private struct Card<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            content
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct ContactChip: Identifiable {
    let id = UUID()
    let name: String
    let role: String
    let status: Presence
}

private struct ActivityRow: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let detail: String
}

private enum Presence {
    case online, away, offline

    var color: Color {
        switch self {
        case .online: return Color.green
        case .away: return Color.yellow
        case .offline: return Color.red
        }
    }

    var label: String {
        switch self {
        case .online: return "Online"
        case .away: return "Idle"
        case .offline: return "Offline"
        }
    }
}

private enum RelayColors {
    // Inspired by the Android gold palette
    static let primary = Color(red: 0.96, green: 0.80, blue: 0.43)   // gold light
    static let accent  = Color(red: 0.75, green: 0.53, blue: 0.12)   // gold deep
    static let deep    = Color(red: 0.07, green: 0.09, blue: 0.14)   // near-black
}

#Preview {
    ContentView(viewModel: AlertRelayViewModel())
}
