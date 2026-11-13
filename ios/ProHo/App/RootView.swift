import SwiftUI

struct RootView: View {
    @Environment(SessionModel.self) private var session

    var body: some View {
        if let token = session.token, let account = session.account, let communityId = session.communityId {
            // A community has its own tabs and bars (CommunityView).
            CommunityView(token: token, account: account, communityId: communityId)
                .id(communityId)
        } else {
            signedOutOrList
        }
    }

    private var signedOutOrList: some View {
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
