import Foundation

// Models for the member's screens: GET /communities/{id}/home and the lists
// behind the Units and Directory sections. Same shapes as
// android/.../net/ApiClient.kt and api/openapi/openapi.yaml.

/// The dashboard, the same on web, Android and iOS.
public struct Home: Codable, Equatable, Sendable {
    public let community: Community
    public let membershipId: String
    public let personId: String?
    public let roles: [String]
    public let units: [MyUnit]
    public let administration: AdministrationSummary?

    public var isAdministrator: Bool { roles.contains("administrator") }
}

/// One of the caller's own units today, with the roles they hold on it.
public struct MyUnit: Codable, Equatable, Identifiable, Sendable {
    public let unitId: String
    public let number: String
    public let kind: String
    public let building: String?
    public let roles: [String]

    public var id: String { unitId }

    public init(unitId: String, number: String, kind: String, building: String?, roles: [String]) {
        self.unitId = unitId
        self.number = number
        self.kind = kind
        self.building = building
        self.roles = roles
    }
}

/// Counts for administrators. Archived and ended records aren't counted.
public struct AdministrationSummary: Codable, Equatable, Sendable {
    public let buildings: Int
    public let units: Int
    public let people: Int
    public let members: Int
    public let suspendedMembers: Int
    public let pendingInvitations: Int

    public init(buildings: Int, units: Int, people: Int, members: Int, suspendedMembers: Int, pendingInvitations: Int) {
        self.buildings = buildings
        self.units = units
        self.people = people
        self.members = members
        self.suspendedMembers = suspendedMembers
        self.pendingInvitations = pendingInvitations
    }
}

public struct PropertyUnit: Codable, Equatable, Identifiable, Sendable {
    public let id: String
    public let number: String
    public let kind: String
    public let buildingId: String?
    public let floor: String?
}

/// A person as the caller may see them: fields they can't see are nil.
public struct Person: Codable, Equatable, Identifiable, Sendable {
    public let id: String
    public let kind: String
    public let givenName: String?
    public let surname: String?
    public let legalName: String?
    public let email: String?
    public let phone: String?

    public var displayName: String {
        kind == "legal" ? (legalName ?? "") : [givenName, surname].compactMap { $0 }.joined(separator: " ")
    }
}

public struct UnitRelationship: Codable, Equatable, Identifiable, Sendable {
    public let id: String
    public let unitId: String
    public let personId: String
    public let role: String
    public let share: Decimal?
    public let validFrom: String
    public let validTo: String?
}

struct CreateUnitBody: Encodable {
    let number: String
    let kind: String
}

struct CreatePersonBody: Encodable {
    var kind = "natural"
    let givenName: String
    let surname: String
}

struct CreateRelationshipBody: Encodable {
    let personId: String
    let role: String
}
