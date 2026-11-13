import SwiftUI

struct RootView: View {
    @Environment(SessionModel.self) private var session

    var body: some View {
        NavigationStack {
            Group {
                if let token = session.token, let account = session.account {
                    CommunitiesView(token: token, account: account)
                } else {
                    AuthView()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 0) {
                        Text(verbatim: "ProHo").font(.headline)
                        Text("tagline").font(.caption2)
                    }
                    .foregroundStyle(.white)
                }
            }
            .toolbarBackground(Theme.ink, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}
