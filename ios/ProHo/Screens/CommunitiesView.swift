import SwiftUI
import ProHoKit

/// The caller's communities, plus "create community". Mirrors
/// android/.../CommunitiesScreen.kt; this is the seed of the community
/// switcher in docs/architecture/client-screens.md.
struct CommunitiesView: View {
    let token: String
    let account: Account

    @Environment(SessionModel.self) private var session
    @State private var communities: [Community]?
    @State private var showCreate = false
    @State private var refreshKey = 0

    var body: some View {
        List {
            Section {
                Label(account.displayName, systemImage: "person.crop.circle")
            }
            Section(header: Text("your_communities_title")) {
                if let communities {
                    if communities.isEmpty {
                        Text("no_communities_yet").foregroundStyle(.secondary)
                    }
                    ForEach(communities) { community in
                        VStack(alignment: .leading) {
                            Text(community.name).font(.headline)
                            Text(community.key).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                } else {
                    HStack { ProgressView(); Text("loading") }
                }
                Button("create_community_action") { showCreate = true }
            }
        }
        .refreshable { await load() }
        .task(id: refreshKey) { await load() }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("logout_action") { session.logOut() }
            }
        }
        .sheet(isPresented: $showCreate) {
            CreateCommunityView(token: token) {
                showCreate = false
                refreshKey += 1
            }
        }
    }

    private func load() async {
        communities = (try? await session.api.listCommunities(token: token)) ?? []
    }
}

struct CreateCommunityView: View {
    let token: String
    let onCreated: () -> Void

    @Environment(SessionModel.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var key = ""
    @State private var errorKey: String?
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                TextField("community_name_label", text: $name)
                TextField("community_key_label", text: $key)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onChange(of: key) { _, newValue in key = newValue.lowercased() }
                if let errorKey {
                    Text(LocalizedStringKey(errorKey)).foregroundStyle(.red)
                }
            }
            .navigationTitle("create_community_action")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel_action") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("create_action", action: create).disabled(busy)
                }
            }
        }
    }

    private func create() {
        errorKey = nil
        busy = true
        Task {
            defer { busy = false }
            do {
                _ = try await session.api.createCommunity(token: token, key: key, name: name)
                onCreated()
            } catch let error as APIError {
                errorKey = error.messageKey
            } catch {
                errorKey = "error_unknown"
            }
        }
    }
}
