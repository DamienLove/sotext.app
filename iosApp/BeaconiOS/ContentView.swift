import SwiftUI

struct ContentView: View {
    @ObservedObject var viewModel: BeaconViewModel

    var isPro: Bool {
        #if PRO
        return true
        #else
        return false
        #endif
    }

    var body: some View {
        TabView {
            InboxTab(viewModel: viewModel)
                .tabItem {
                    Label("Inbox", systemImage: "bubble.left.and.bubble.right.fill")
                }
                .badge(isPro ? "Pro" : nil)

            SettingsTab()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }
        }
    }
}

private struct InboxTab: View {
    @ObservedObject var viewModel: BeaconViewModel

    var isPro: Bool {
        #if PRO
        return true
        #else
        return false
        #endif
    }

    var body: some View {
        NavigationStack {
            List(viewModel.contacts) { contact in
                NavigationLink(value: contact) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(contact.name).font(.headline)
                            Text(contact.role).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        if contact.unread > 0 {
                            Text("\(contact.unread)")
                                .font(.caption.bold())
                                .foregroundStyle(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.blue)
                                .clipShape(Capsule())
                        }
                    }
                }
            }
            .navigationDestination(for: BeaconContactCard.self) { contact in
                ConversationView(
                    contact: contact,
                    messages: viewModel.messages(for: contact),
                    onSend: { text in
                        viewModel.sendMessage(to: contact, text: text)
                    }
                )
            }
            .navigationTitle(isPro ? "Beacon Inbox Pro" : "Beacon Inbox")
        }
    }
}

private struct ConversationView: View {
    let contact: BeaconContactCard
    let messages: [BeaconConversationMessage]
    let onSend: (String) -> Void

    @State private var draft = ""

    var body: some View {
        VStack {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(messages) { msg in
                            HStack {
                                if msg.isIncoming { Spacer() }
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(msg.text)
                                        .padding(12)
                                        .background(msg.isIncoming ? Color(.secondarySystemBackground) : Color.blue)
                                        .foregroundStyle(msg.isIncoming ? .primary : .white)
                                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    Text(msg.timestamp, style: .time)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                if !msg.isIncoming { Spacer() }
                            }
                            .id(msg.id)
                        }
                    }
                    .padding()
                }
                .onAppear {
                    if let last = messages.last { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }

            HStack {
                TextField("Message", text: $draft)
                    .textFieldStyle(.roundedBorder)
                Button {
                    guard !draft.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                    onSend(draft)
                    draft = ""
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
            .background(.thinMaterial)
        }
        .navigationTitle(contact.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SettingsTab: View {
    var body: some View {
        NavigationStack {
            Form {
                Section("Appearance") {
                    Text("Theme settings coming soon")
                }
                Section("About") {
                    Text("Beacon iOS")
                }
            }
            .navigationTitle("Settings")
        }
    }
}
