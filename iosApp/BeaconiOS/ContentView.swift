import SwiftUI
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

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
        if viewModel.isLoggedIn {
            TabView {
                BeaconTab(viewModel: viewModel, filter: .inbox)
                    .tabItem {
                        Label("Inbox", systemImage: "bubble.left.and.bubble.right.fill")
                    }
                    .badge(isPro ? "Pro" : nil)

                BeaconTab(viewModel: viewModel, filter: .trusted)
                    .tabItem {
                        Label("Trusted", systemImage: "shield.fill")
                    }

                BeaconTab(viewModel: viewModel, filter: .favorites)
                    .tabItem {
                        Label("Favorites", systemImage: "star.fill")
                    }

                if isPro {
                    BeaconTab(viewModel: viewModel, filter: .private)
                        .tabItem {
                            Label("Private", systemImage: "lock.fill")
                        }
                }

                SettingsTab(viewModel: viewModel, isPro: isPro)
                    .tabItem {
                        Label("Settings", systemImage: "gear")
                    }
            }
        } else {
            LoginView {
                // Auth listener will handle transition
            }
        }
    }
}

enum BeaconTabFilter {
    case inbox, trusted, favorites, `private`

    var title: String {
        switch self {
        case .inbox: return "Beacon Inbox"
        case .trusted: return "Trusted"
        case .favorites: return "Favorites"
        case .private: return "Private Safe"
        }
    }
}

private struct BeaconTab: View {
    @ObservedObject var viewModel: BeaconViewModel
    let filter: BeaconTabFilter
    @State private var searchText = ""
    @State private var subFilter: InboxSubFilter = .all
    @State private var pinInput = ""
    @State private var isUnlocked = false
    @State private var showPinSheet = false
    @State private var pinError: String?
    @State private var shakeOffset: CGFloat = 0
    @AppStorage("themeColor") private var themeColor: ThemeColor = .blue
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded
    @AppStorage("privateSafeAutoLockEnabled") private var autoLockEnabled: Bool = true
    @State private var autoLockTask: Task<Void, Never>?

    private let minimumPinLength = 4

    private var storedPin: String? {
        KeychainHelper.loadPin()
    }

    enum InboxSubFilter: String, CaseIterable {
        case all = "All"
        case read = "Read"
        case unread = "Unread"
    }

    var filteredContacts: [BeaconContactCard] {
        let base: [BeaconContactCard]
        switch filter {
        case .inbox: base = viewModel.inboxContacts
        case .trusted: base = viewModel.trustedContacts
        case .favorites: base = viewModel.favoriteContacts
        case .private: base = viewModel.privateContacts
        }

        let subFiltered: [BeaconContactCard]
        if filter != .private {
            switch subFilter {
            case .all: subFiltered = base
            case .read: subFiltered = base.filter { $0.unread == 0 }
            case .unread: subFiltered = base.filter { $0.unread > 0 }
            }
        } else {
            subFiltered = base
        }

        if searchText.isEmpty {
            return subFiltered
        } else {
            return subFiltered.filter { $0.name.localizedCaseInsensitiveContains(searchText) || $0.address.contains(searchText) }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if filter != .private {
                    Picker("Filter", selection: $subFilter) {
                        ForEach(InboxSubFilter.allCases, id: \.self) { f in
                            Text(f.rawValue).tag(f)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .padding(.top, 10)
                    .accessibilityLabel("Message filter")
                    .accessibilityHint("Filter messages by read status")
                }

                if filter == .private && !isUnlocked {
                    VStack(spacing: 20) {
                        Image(systemName: "lock.circle.fill")
                            .font(.system(size: 60))
                            .foregroundStyle(.secondary)
                        Text(storedPin == nil ? "Setup Private Safe" : "Private Safe Locked")
                            .font(.title2.bold())
                        Button(storedPin == nil ? "Set PIN" : "Unlock") {
                            showPinSheet = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .frame(maxHeight: .infinity)
                } else {
                    List(filteredContacts) { contact in
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
                                        .background(themeColor.color)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                    .searchable(text: $searchText)
                    .overlay {
                        if filteredContacts.isEmpty {
                            let emptyMessage: String
                            if filter != .private && subFilter != .all {
                                emptyMessage = subFilter == .read ? "No read messages" : "No unread messages"
                            } else {
                                emptyMessage = "No conversations"
                            }
                            ContentUnavailableView(
                                emptyMessage,
                                systemImage: "bubble.left.and.bubble.right",
                                description: Text(filter != .private ? "Try adjusting the filter" : "Start a new chat on your Android device.")
                            )
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
            .navigationTitle(filter.title)
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
                if filter == .private && autoLockEnabled {
                    isUnlocked = false
                }
            }
            .onChange(of: isUnlocked) { newValue in
                if newValue && filter == .private && autoLockEnabled {
                    // Auto-lock after 5 minutes of inactivity
                    autoLockTask?.cancel()
                    autoLockTask = Task {
                        try? await Task.sleep(nanoseconds: 5 * 60 * 1_000_000_000)
                        if !Task.isCancelled {
                            isUnlocked = false
                        }
                    }
                } else {
                    autoLockTask?.cancel()
                }
            }
            .sheet(isPresented: $showPinSheet) {
                NavigationStack {
                    VStack(spacing: 20) {
                        Text(storedPin == nil ? "Create a PIN" : "Enter PIN")
                            .font(.headline)

                        VStack(spacing: 8) {
                            SecureField("PIN", text: $pinInput)
                                .keyboardType(.numberPad)
                                .padding()
                                .background(Color(.secondarySystemBackground))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .padding(.horizontal)
                                .offset(x: shakeOffset)

                            if let error = pinError {
                                Text(error)
                                    .font(.caption)
                                    .foregroundStyle(.red)
                                    .padding(.horizontal)
                            }
                        }

                        Button(storedPin == nil ? "Save PIN" : "Unlock") {
                            if storedPin == nil {
                                // Creating new PIN - validate strength
                                let validation = KeychainHelper.isValidPin(pinInput)
                                if validation.isValid {
                                    do {
                                        try KeychainHelper.savePin(pinInput)
                                        isUnlocked = true
                                        showPinSheet = false
                                        pinInput = ""
                                        pinError = nil
                                    } catch {
                                        pinError = "Failed to save PIN"
                                        triggerShake()
                                    }
                                } else {
                                    pinError = validation.reason
                                    triggerShake()
                                }
                            } else {
                                // Unlocking - verify PIN
                                if pinInput == storedPin {
                                    isUnlocked = true
                                    showPinSheet = false
                                    pinInput = ""
                                    pinError = nil
                                } else {
                                    pinError = "Incorrect PIN"
                                    triggerShake()
                                    // Haptic feedback
                                    let generator = UINotificationFeedbackGenerator()
                                    generator.notificationOccurred(.error)
                                    pinInput = ""
                                }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(pinInput.count < minimumPinLength)
                    }
                    .padding()
                    .presentationDetents([.height(350)])
                    .onDisappear {
                        pinInput = ""
                        pinError = nil
                    }
                }
            }
        }
    }

    private func triggerShake() {
        let animation = Animation.easeInOut(duration: 0.1)
        withAnimation(animation) {
            shakeOffset = 10
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(animation) {
                shakeOffset = -10
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            withAnimation(animation) {
                shakeOffset = 5
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            withAnimation(animation) {
                shakeOffset = 0
            }
        }
    }
}

private struct ConversationView: View {
    let contact: BeaconContactCard
    let messages: [BeaconConversationMessage]
    let onSend: (String) -> Void

    @State private var draft = ""
    @AppStorage("themeColor") private var themeColor: ThemeColor = .blue
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded

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
                                        .background(msg.isIncoming ? Color(.secondarySystemBackground) : themeColor.color)
                                        .foregroundStyle(msg.isIncoming ? .primary : .white)
                                        .clipShape(bubbleStyle.shape)
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
    @ObservedObject var viewModel: BeaconViewModel
    let isPro: Bool
    @AppStorage("themeColor") private var themeColor: ThemeColor = .blue
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded
    @AppStorage("privateSafeAutoLockEnabled") private var autoLockEnabled: Bool = true
    @State private var showResetPinConfirmation = false

    var body: some View {
        NavigationStack {
            Form {
                if isPro {
                    Section("Appearance") {
                        Picker("Accent Color", selection: $themeColor) {
                            ForEach(ThemeColor.allCases, id: \.self) { color in
                                Text(color.rawValue.capitalized).tag(color)
                            }
                        }
                        Picker("Bubble Style", selection: $bubbleStyle) {
                            ForEach(BubbleStyle.allCases, id: \.self) { style in
                                Text(style.rawValue.capitalized).tag(style)
                            }
                        }
                    }

                    Section {
                        Toggle("Auto-lock Private Safe", isOn: $autoLockEnabled)
                        Button("Reset Private Safe PIN", role: .destructive) {
                            showResetPinConfirmation = true
                        }
                    } header: {
                        Text("Private Safe")
                    } footer: {
                        Text("Auto-lock will lock Private Safe when app goes to background or after 5 minutes of inactivity")
                    }
                } else {
                    Section("Appearance") {
                        Text("Upgrade to Pro to customize themes.")
                            .foregroundStyle(.secondary)
                    }
                }
                Section("Account") {
                    Button("Sign Out", role: .destructive) {
                        #if canImport(FirebaseAuth)
                        try? FirebaseAuth.Auth.auth().signOut()
                        #endif
                    }
                }
                Section("About") {
                    Text("Beacon iOS")
                }
            }
            .navigationTitle("Settings")
            .alert("Reset PIN", isPresented: $showResetPinConfirmation) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) {
                    try? KeychainHelper.deletePin()
                }
            } message: {
                Text("This will delete your Private Safe PIN. You'll need to set a new PIN to access Private Safe.")
            }
        }
    }
}

enum ThemeColor: String, CaseIterable {
    case blue, purple, orange, green, pink

    var color: Color {
        switch self {
        case .blue: return .blue
        case .purple: return .purple
        case .orange: return .orange
        case .green: return .green
        case .pink: return .pink
        }
    }
}

enum BubbleStyle: String, CaseIterable {
    case rounded, square, capsule

    var shape: AnyShape {
        switch self {
        case .rounded: return AnyShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        case .square: return AnyShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
        case .capsule: return AnyShape(Capsule())
        }
    }
}
