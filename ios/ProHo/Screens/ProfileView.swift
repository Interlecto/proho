import SwiftUI
import ProHoKit

/// The caller's account and their own record in this community. What they
/// share is edited on the web. Mirrors android/.../ui/screens/ProfileScreen.kt.
struct ProfileView: View {
    let token: String
    let communityId: String
    let account: Account

    private enum Load {
        case loading, unlinked, failed
        case ready(Person)
    }

    @Environment(SessionModel.self) private var session
    @State private var state = Load.loading

    var body: some View {
        List {
            Section(header: Text("profile_account_title")) {
                Text(verbatim: account.displayName)
                Text(verbatim: account.email).foregroundStyle(.secondary)
            }
            Section(header: Text("profile_community_title")) {
                switch state {
                case .loading:
                    ProgressView()
                case .unlinked:
                    Text("home_not_linked").foregroundStyle(.secondary)
                case .failed:
                    Text("error_load").foregroundStyle(.red)
                case let .ready(person):
                    Text(verbatim: person.displayName)
                    ForEach([person.email, person.phone].compactMap { $0 }, id: \.self) { contact in
                        Text(verbatim: contact).foregroundStyle(.secondary)
                    }
                    Text("profile_edit_on_web").font(.caption).foregroundStyle(.secondary)
                }
            }
            Section {
                Button("switch_community") { session.leaveCommunity() }
                Button("logout_action", role: .destructive) { session.logOut() }
            }
        }
        .task {
            do {
                if let person = try await session.api.profile(token: token, communityId: communityId) {
                    state = .ready(person)
                } else {
                    state = .unlinked
                }
            } catch {
                state = .failed
            }
        }
    }
}
