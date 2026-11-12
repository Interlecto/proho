import SwiftUI
import ProHoKit

/// The community's units, read-only for members. An administrator adds units
/// and relationships. Mirrors android/.../ui/screens/UnitsScreen.kt.
struct UnitsView: View {
    let token: String
    let communityId: String
    let isAdministrator: Bool
    @Binding var version: Int

    @Environment(SessionModel.self) private var session
    @State private var units: [PropertyUnit]?
    @State private var failed = false
    @State private var adding = false

    var body: some View {
        Group {
            if let units {
                if units.isEmpty {
                    Text("no_units").foregroundStyle(.secondary)
                } else {
                    List(units) { unit in
                        NavigationLink {
                            UnitDetailView(token: token, communityId: communityId, unit: unit,
                                           isAdministrator: isAdministrator)
                        } label: {
                            VStack(alignment: .leading) {
                                Text(verbatim: unit.number).font(.headline)
                                Text(verbatim: localized(LabelKey.unitKind(unit.kind), unit.kind))
                                    .font(.caption).foregroundStyle(.secondary)
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
            if MemberSection.units.canAdd(isAdministrator: isAdministrator) {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { adding = true } label: { Image(systemName: "plus") }
                        .accessibilityLabel(Text("add_unit"))
                }
            }
        }
        .sheet(isPresented: $adding) {
            CreateUnitView(token: token, communityId: communityId) { version += 1 }
        }
    }

    private func load() async {
        failed = false
        do {
            units = try await session.api.listUnits(token: token, communityId: communityId)
                .sorted { $0.number.localizedStandardCompare($1.number) == .orderedAscending }
        } catch {
            failed = true
        }
    }
}

private struct UnitDetailView: View {
    let token: String
    let communityId: String
    let unit: PropertyUnit
    let isAdministrator: Bool

    @Environment(SessionModel.self) private var session
    @State private var relationships: [UnitRelationship]?
    @State private var people: [String: Person] = [:]
    @State private var failed = false
    @State private var adding = false
    @State private var reload = 0

    var body: some View {
        List {
            Section {
                Text(verbatim: localized(LabelKey.unitKind(unit.kind), unit.kind))
                if let floor = unit.floor { Text(verbatim: floor).foregroundStyle(.secondary) }
            }
            Section(header: Text("unit_relationships_title")) {
                if let relationships {
                    if relationships.isEmpty {
                        Text("no_relationships").foregroundStyle(.secondary)
                    }
                    ForEach(relationships) { relationship in
                        VStack(alignment: .leading, spacing: 2) {
                            if let name = people[relationship.personId]?.displayName, !name.isEmpty {
                                Text(verbatim: name)
                            } else {
                                Text("person_hidden").foregroundStyle(.secondary)
                            }
                            Text(verbatim: describe(relationship)).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                } else if failed {
                    LoadFailedView { reload += 1 }
                } else {
                    ProgressView()
                }
                if isAdministrator {
                    Button("add_relationship") { adding = true }
                }
            }
        }
        .navigationTitle(Text(verbatim: unit.number))
        .task(id: reload) { await load() }
        .sheet(isPresented: $adding) {
            AddRelationshipView(token: token, communityId: communityId, unitId: unit.id,
                                people: people.values.sorted { $0.displayName < $1.displayName }) { reload += 1 }
        }
    }

    private func describe(_ relationship: UnitRelationship) -> String {
        var parts = [localized(LabelKey.relationshipRole(relationship.role), relationship.role)]
        if let share = relationship.share { parts.append("\(share) %") }
        if let validTo = relationship.validTo { parts.append("\(relationship.validFrom) – \(validTo)") }
        return parts.joined(separator: " · ")
    }

    private func load() async {
        failed = false
        do {
            relationships = try await session.api.listUnitRelationships(token: token, communityId: communityId,
                                                                        unitId: unit.id)
            let list = try await session.api.listPeople(token: token, communityId: communityId)
            people = Dictionary(uniqueKeysWithValues: list.map { ($0.id, $0) })
        } catch {
            failed = true
        }
    }
}

private struct CreateUnitView: View {
    let token: String
    let communityId: String
    let onSaved: () -> Void

    @Environment(SessionModel.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var number = ""
    @State private var kind = "apartment"
    @State private var errorKey: String?
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                TextField("unit_number_label", text: $number)
                Picker("unit_kind_label", selection: $kind) {
                    ForEach(LabelKey.unitKinds, id: \.self) { code in
                        Text(verbatim: localized(LabelKey.unitKind(code), code)).tag(code)
                    }
                }
                if let errorKey { Text(LocalizedStringKey(errorKey)).foregroundStyle(.red) }
            }
            .navigationTitle("add_unit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel_action") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save_action", action: save)
                        .disabled(busy || number.trimmingCharacters(in: .whitespaces).isEmpty)
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
                _ = try await session.api.createUnit(token: token, communityId: communityId,
                                                     number: number.trimmingCharacters(in: .whitespaces), kind: kind)
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

private struct AddRelationshipView: View {
    let token: String
    let communityId: String
    let unitId: String
    let people: [Person]
    let onSaved: () -> Void

    @Environment(SessionModel.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var personId: String?
    @State private var role = "resident"
    @State private var errorKey: String?
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                Picker("person_label", selection: $personId) {
                    Text("choose_option").tag(String?.none)
                    ForEach(people) { person in
                        Text(verbatim: person.displayName).tag(Optional(person.id))
                    }
                }
                Picker("relationship_role_label", selection: $role) {
                    ForEach(LabelKey.relationshipRoles, id: \.self) { code in
                        Text(verbatim: localized(LabelKey.relationshipRole(code), code)).tag(code)
                    }
                }
                if let errorKey { Text(LocalizedStringKey(errorKey)).foregroundStyle(.red) }
            }
            .navigationTitle("add_relationship")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel_action") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save_action", action: save).disabled(busy || personId == nil)
                }
            }
        }
    }

    private func save() {
        guard let personId else { return }
        errorKey = nil
        busy = true
        Task {
            defer { busy = false }
            do {
                _ = try await session.api.createRelationship(token: token, communityId: communityId, unitId: unitId,
                                                             personId: personId, role: role)
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
