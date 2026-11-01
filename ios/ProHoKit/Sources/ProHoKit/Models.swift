import Foundation

// Mirrors the backend's response DTOs (backend/src/main/kotlin/...) and
// api/openapi/openapi.yaml. Unknown fields are ignored by Decodable, so the
// backend can add fields without breaking older app builds.

public struct Account: Codable, Equatable, Identifiable, Sendable {
    public let id: String
    public let email: String
    public let displayName: String
    public let status: String

    public init(id: String, email: String, displayName: String, status: String) {
        self.id = id
        self.email = email
        self.displayName = displayName
        self.status = status
    }
}

public struct Community: Codable, Equatable, Identifiable, Sendable {
    public let id: String
    public let key: String
    public let name: String
    public let kind: String
    public let legalName: String?
    public let taxId: String?
    public let plan: String?
    public let country: String?
    public let currency: String?
    public let timezone: String?
    public let address: String?

    public init(
        id: String,
        key: String,
        name: String,
        kind: String,
        legalName: String? = nil,
        taxId: String? = nil,
        plan: String? = nil,
        country: String? = nil,
        currency: String? = nil,
        timezone: String? = nil,
        address: String? = nil
    ) {
        self.id = id
        self.key = key
        self.name = name
        self.kind = kind
        self.legalName = legalName
        self.taxId = taxId
        self.plan = plan
        self.country = country
        self.currency = currency
        self.timezone = timezone
        self.address = address
    }
}

public struct LoginResult: Codable, Equatable, Sendable {
    public let token: String
    public let expiresAt: Date
    public let account: Account
}

// MARK: - Request bodies

struct RegisterAccountBody: Encodable {
    let email: String
    let password: String
    let displayName: String
}

struct LoginBody: Encodable {
    let email: String
    let password: String
}

struct PasswordResetRequestBody: Encodable {
    let email: String
}

struct PasswordResetConfirmBody: Encodable {
    let token: String
    let newPassword: String
}

struct CreateCommunityBody: Encodable {
    let key: String
    let name: String
    let kind: String
}

struct ErrorBody: Decodable {
    let error: String
}
