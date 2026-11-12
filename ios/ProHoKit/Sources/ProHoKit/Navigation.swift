/// The sections every member sees, in the same order as the web and Android
/// clients (docs/architecture/client-screens.md). The web's administration
/// sections stay on the web; an administrator gets the trimmed actions inside
/// Units and Directory. Not called `Section`, which SwiftUI already uses.
public enum MemberSection: String, CaseIterable, Sendable {
    case home, units, directory, profile

    /// Key into Localizable.strings, the same name as Android's string resource.
    public var titleKey: String { "section_\(rawValue)" }

    /// SF Symbol for the tab.
    public var systemImage: String {
        switch self {
        case .home: return "house"
        case .units: return "building.2"
        case .directory: return "person.2"
        case .profile: return "person.crop.circle"
        }
    }

    /// Where an administrator gets an add button.
    public func canAdd(isAdministrator: Bool) -> Bool {
        isAdministrator && (self == .units || self == .directory)
    }
}

/// Labels for codes the API returns, as Localizable.strings keys. An unknown
/// code has no key, and the app shows the code itself.
public enum LabelKey {
    public static let unitKinds = ["apartment", "house", "commercial", "office", "parking", "storage"]
    public static let relationshipRoles = ["owner", "tenant", "resident", "household_member", "representative"]
    public static let assignedRoles = [
        "administrator", "accountant", "treasurer", "auditor", "council_member", "moderator", "staff",
    ]

    public static func unitKind(_ code: String) -> String? { unitKinds.contains(code) ? "kind_\(code)" : nil }

    public static func relationshipRole(_ code: String) -> String? {
        relationshipRoles.contains(code) ? "relationship_\(code)" : nil
    }

    public static func assignedRole(_ code: String) -> String? { assignedRoles.contains(code) ? "role_\(code)" : nil }
}
