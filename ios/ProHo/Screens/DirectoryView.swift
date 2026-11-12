import SwiftUI
import ProHoKit

/// The people the caller may see (docs/security/privacy.md). The backend
/// does the filtering. Mirrors android/.../ui/screens/DirectoryScreen.kt.
struct DirectoryView: View {
    let token: String
    let communityId: String
    let isAdministrator: Bool
    @Binding var version: Int

    @Environment(SessionModel.self) private var session
    @State private var people: [Person]?
    @State private var failed = false
    @State private var adding = false

    var body: some View {
        Group {
            if let people {
                if people.isEmpty {
                    Text("no_people").foregroundStyle(.secondary)
                } else {
                    List(people) { person in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: person.displayName).font(.headline)
                            ForEach([person.email, person.phone].compactMap { $0 }, id: \.self) { contact in
                                Text(verbatim: contact).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            } else if failed {
                LoadFailedView { Task { await load() } }
            } else {
                ProgressView()
            }
        }
        .task(id: version) { await load() }
        .refreshable { await load() }
        .toolbar {
            if MemberSection.directory.canAdd(isAdministrator: isAdministrator) {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { adding = true } label: { Image(systemName: "plus") }
                        .accessibilityLabel(Text("add_person"))
                }
            }
        }
        .sheet(isPresented: $adding) {
            CreatePersonView(token: token, communityId: communityId) { version += 1 }
        }
    }

    private func load() async {
        failed = false
        do {
            people = try await session.api.listPeople(token: token, communityId: communityId)
                .filter { !$0.displayName.isEmpty }
                .sorted { $0.displayName.localizedCompare($1.displayName) == .orderedAscending }
        } catch {
            failed = true
        }
    }
}

private struct CreatePersonView: View {
    let token: String
    let communityId: String
    let onSaved: () -> Void

    @Environment(SessionModel.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var givenName = ""
    @State private var surname = ""
    @State private var errorKey: String?
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                TextField("given_name_label", text: $givenName).textContentType(.givenName)
                TextField("surname_label", text: $surname).textContentType(.familyName)
                if let errorKey { Text(LocalizedStringKey(errorKey)).foregroundStyle(.red) }
            }
            .navigationTitle("add_person")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel_action") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save_action", action: save).disabled(busy || givenName.isBlank || surname.isBlank)
                }
            }
        }
    }

    private func save() {
        errorKey = nil
        busy = true
        Task {
            defer { busy = false }
            do {
                _ = try await session.api.createPerson(
                    token: token, communityId: communityId,
                    givenName: givenName.trimmingCharacters(in: .whitespaces),
                    surname: surname.trimmingCharacters(in: .whitespaces))
                onSaved()
                dismiss()
            } catch let error as APIError {
                errorKey = error.messageKey
            } catch {
                errorKey = "error_unknown"
            }
        }
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
