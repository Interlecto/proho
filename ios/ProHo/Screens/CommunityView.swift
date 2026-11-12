import SwiftUI
import ProHoKit

/// One community: the shared sections as tabs, and a community switcher in
/// the navigation bar. Mirrors android/.../ui/screens/CommunityScreen.kt.
struct CommunityView: View {
    let token: String
    let account: Account
    let communityId: String

    @Environment(SessionModel.self) private var session
    @State private var section = MemberSection.home
    @State private var home: Home?
    @State private var failed = false
    @State private var communities: [Community] = []
    /// Bumped after a change, so Home's counts and the lists load again.
    @State private var version = 0

    var body: some View {
        TabView(selection: $section) {
            ForEach(MemberSection.allCases, id: \.self) { entry in
                NavigationStack {
                    content(for: entry)
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .principal) { switcher }
                        }
                        .toolbarBackground(Theme.ink, for: .navigationBar)
                        .toolbarBackground(.visible, for: .navigationBar)
                        .toolbarColorScheme(.dark, for: .navigationBar)
                }
                .tabItem { Label(LocalizedStringKey(entry.titleKey), systemImage: entry.systemImage) }
                .tag(entry)
            }
        }
        .task(id: version) { await loadHome() }
        .task { communities = (try? await session.api.listCommunities(token: token)) ?? [] }
        .onChange(of: section) { _, newValue in
            if newValue == .home { version += 1 }
        }
    }

    @ViewBuilder
    private func content(for entry: MemberSection) -> some View {
        if let home {
            switch entry {
            case .home:
                HomeView(home: home)
            case .units:
                UnitsView(token: token, communityId: communityId, isAdministrator: home.isAdministrator,
                          version: $version)
            case .directory:
                DirectoryView(token: token, communityId: communityId, isAdministrator: home.isAdministrator,
                              version: $version)
            case .profile:
                ProfileView(token: token, communityId: communityId, account: account)
            }
        } else if failed {
            LoadFailedView { version += 1 }
        } else {
            ProgressView()
        }
    }

    private var switcher: some View {
        Menu {
            ForEach(communities.filter { $0.id != communityId }) { community in
                Button(community.name) { session.open(community: community.id) }
            }
            Divider()
            Button("all_communities") { session.leaveCommunity() }
        } label: {
            HStack(spacing: 4) {
                Text(verbatim: home?.community.name ?? "…").font(.headline)
                Image(systemName: "chevron.down").font(.caption)
            }
            .foregroundStyle(.white)
        }
        .accessibilityLabel(Text("switch_community"))
    }

    private func loadHome() async {
        failed = false
        do {
            home = try await session.api.home(token: token, communityId: communityId)
        } catch let error as APIError where error.code == "membership_not_active" {
            session.leaveCommunity()
        } catch {
            failed = true
        }
    }
}

struct LoadFailedView: View {
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("error_load").foregroundStyle(.red)
            Button("retry_action", action: retry)
        }
        .padding()
    }
}

/// The label for a code the API returns, or the code itself when it's unknown.
func localized(_ key: String?, _ code: String) -> String {
    key.map { String(localized: String.LocalizationValue($0)) } ?? code
}
