import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Typed client for the ProHo REST API (api/openapi/openapi.yaml).
/// Same surface as android/.../net/ApiClient.kt; grow both together.
public struct APIClient: Sendable {
    /// e.g. `http://127.0.0.1:8080` in development or
    /// `https://api.ilecto.net/proho` in production. Paths are *appended*,
    /// so a base with a path prefix works.
    public let baseURL: URL
    private let transport: any HTTPTransport

    public init(baseURL: URL, transport: any HTTPTransport = URLSessionTransport()) {
        self.baseURL = baseURL
        self.transport = transport
    }

    // MARK: identity

    public func register(email: String, password: String, displayName: String) async throws -> Account {
        try await call("POST", "accounts",
                       body: RegisterAccountBody(email: email, password: password, displayName: displayName))
    }

    public func login(email: String, password: String) async throws -> LoginResult {
        try await call("POST", "auth/login", body: LoginBody(email: email, password: password))
    }

    public func me(token: String) async throws -> Account {
        try await call("GET", "me", token: token)
    }

    public func logout(token: String) async throws {
        _ = try await raw("POST", "auth/logout", body: nil, token: token)
    }

    public func requestPasswordReset(email: String) async throws {
        _ = try await raw("POST", "auth/password-reset/request",
                          body: PasswordResetRequestBody(email: email), token: nil)
    }

    public func confirmPasswordReset(token resetToken: String, newPassword: String) async throws {
        _ = try await raw("POST", "auth/password-reset/confirm",
                          body: PasswordResetConfirmBody(token: resetToken, newPassword: newPassword), token: nil)
    }

    // MARK: communities

    public func listCommunities(token: String) async throws -> [Community] {
        try await call("GET", "communities", token: token)
    }

    public func createCommunity(token: String, key: String, name: String,
                                kind: String = "ph_residential") async throws -> Community {
        try await call("POST", "communities", body: CreateCommunityBody(key: key, name: name, kind: kind), token: token)
    }

    // MARK: member screens

    public func home(token: String, communityId: String) async throws -> Home {
        try await call("GET", "communities/\(communityId)/home", token: token)
    }

    public func listUnits(token: String, communityId: String) async throws -> [PropertyUnit] {
        try await call("GET", "communities/\(communityId)/units", token: token)
    }

    public func listUnitRelationships(token: String, communityId: String, unitId: String) async throws -> [UnitRelationship] {
        try await call("GET", "communities/\(communityId)/units/\(unitId)/relationships", token: token)
    }

    public func listPeople(token: String, communityId: String) async throws -> [Person] {
        try await call("GET", "communities/\(communityId)/people", token: token)
    }

    /// The caller's own record, or nil when no person is linked to their membership.
    public func profile(token: String, communityId: String) async throws -> Person? {
        do {
            let person: Person = try await call("GET", "communities/\(communityId)/profile", token: token)
            return person
        } catch let error as APIError where error.code == "profile_not_linked" {
            return nil
        }
    }

    public func createUnit(token: String, communityId: String, number: String, kind: String) async throws -> PropertyUnit {
        try await call("POST", "communities/\(communityId)/units",
                       body: CreateUnitBody(number: number, kind: kind), token: token)
    }

    public func createPerson(token: String, communityId: String, givenName: String, surname: String) async throws -> Person {
        try await call("POST", "communities/\(communityId)/people",
                       body: CreatePersonBody(givenName: givenName, surname: surname), token: token)
    }

    public func createRelationship(token: String, communityId: String, unitId: String,
                                   personId: String, role: String) async throws -> UnitRelationship {
        try await call("POST", "communities/\(communityId)/units/\(unitId)/relationships",
                       body: CreateRelationshipBody(personId: personId, role: role), token: token)
    }

    // MARK: plumbing

    private func call<Response: Decodable>(
        _ method: String, _ path: String, body: (any Encodable)? = nil, token: String? = nil
    ) async throws -> Response {
        let data = try await raw(method, path, body: body, token: token)
        do {
            return try APIClient.decoder.decode(Response.self, from: data)
        } catch {
            throw APIError.decoding(String(describing: error))
        }
    }

    private func raw(_ method: String, _ path: String, body: (any Encodable)?, token: String?) async throws -> Data {
        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let token = token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body = body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try APIClient.encoder.encode(body)
        }

        let (data, response) = try await transport.send(request)
        guard (200..<300).contains(response.statusCode) else {
            let code = (try? JSONDecoder().decode(ErrorBody.self, from: data))?.error ?? "unknown_error"
            throw APIError.server(status: response.statusCode, code: code)
        }
        return data
    }

    static let encoder = JSONEncoder()

    static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let text = try container.decode(String.self)
            guard let date = parseInstant(text) else {
                throw DecodingError.dataCorruptedError(in: container, debugDescription: "Not an ISO-8601 instant: \(text)")
            }
            return date
        }
        return decoder
    }()

    /// Spring/Jackson writes `Instant` as ISO-8601 with 0 to 9 fractional
    /// digits (PostgreSQL timestamps give 6). `ISO8601DateFormatter`'s
    /// `.withFractionalSeconds` handling of more than 3 digits differs
    /// between Apple and swift-corelibs Foundation, so the fraction is split
    /// off and parsed by hand.
    static func parseInstant(_ text: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        guard let dot = text.firstIndex(of: ".") else {
            return formatter.date(from: text)
        }
        let afterDot = text[text.index(after: dot)...]
        let digits = afterDot.prefix { $0.isASCII && $0.isNumber }
        guard !digits.isEmpty,
              let whole = formatter.date(from: String(text[..<dot]) + String(afterDot.dropFirst(digits.count))),
              let fraction = Double("0." + digits)
        else { return nil }
        return whole.addingTimeInterval(fraction)
    }
}
