import SwiftUI
import ProHoKit

/// The dashboard: the same blocks, in the same order, as the web and Android.
struct HomeView: View {
    let home: Home

    var body: some View {
        List {
            if home.community.address != nil || !home.roles.isEmpty {
                Section {
                    if let address = home.community.address {
                        Text(verbatim: address).foregroundStyle(.secondary)
                    }
                    if !home.roles.isEmpty {
                        Text(verbatim: home.roles.map { localized(LabelKey.assignedRole($0), $0) }.joined(separator: " · "))
                    }
                }
            }

            Section(header: Text("my_units_title"), footer: Text("home_billing_placeholder")) {
                if home.personId == nil {
                    Text("home_not_linked").foregroundStyle(.secondary)
                } else if home.units.isEmpty {
                    Text("home_no_units").foregroundStyle(.secondary)
                } else {
                    ForEach(home.units) { unit in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: [unit.building, unit.number].compactMap { $0 }.joined(separator: " · "))
                                .font(.headline)
                            Text(verbatim: localized(LabelKey.unitKind(unit.kind), unit.kind))
                                .font(.caption).foregroundStyle(.secondary)
                            Text(verbatim: unit.roles.map { localized(LabelKey.relationshipRole($0), $0) }
                                .joined(separator: ", "))
                        }
                    }
                }
            }

            if let summary = home.administration {
                Section(header: Text("administration_title"), footer: Text("admin_on_web")) {
                    LabeledContent("count_units", value: "\(summary.units)")
                    LabeledContent("count_people", value: "\(summary.people)")
                    LabeledContent("count_buildings", value: "\(summary.buildings)")
                    LabeledContent("count_members", value: "\(summary.members)")
                    if summary.suspendedMembers > 0 {
                        Text("members_suspended \(summary.suspendedMembers)").foregroundStyle(.secondary)
                    }
                    if summary.pendingInvitations > 0 {
                        Text("invitations_pending \(summary.pendingInvitations)").foregroundStyle(.secondary)
                    }
                }
            }
        }
    }
}
