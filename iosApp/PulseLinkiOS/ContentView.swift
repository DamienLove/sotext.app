import SwiftUI

struct ContentView: View {
    @ObservedObject var viewModel: AlertRelayViewModel

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text("PulseLink Relay")
                    .font(.largeTitle.bold())

                Text(viewModel.statusText)
                    .font(.callout)
                    .foregroundColor(.secondary)

                Button(action: { Task { await viewModel.sendTestAlert() } }) {
                    Text("Send test alert")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Spacer()
            }
            .padding()
            .navigationTitle("PulseLink")
        }
    }
}

#Preview {
    ContentView(viewModel: AlertRelayViewModel())
}
