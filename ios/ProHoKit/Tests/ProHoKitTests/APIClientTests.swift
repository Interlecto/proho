import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
import XCTest
@testable import ProHoKit

/// Records every request and replies with a canned status and body.
actor StubTransport: HTTPTransport {
    private(set) var requests: [URLRequest] = []
    private let status: Int
    private let body: String

    init(status: Int = 200, body: String = "") {
        self.status = status
        self.body = body
    }

    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        requests.append(request)
        let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1", headerFields: nil)!
        return (Data(body.utf8), response)
    }
}

private let accountJSON = """
{"id":"0b6f6d0e-0000-4000-8000-000000000001","email":"ana@example.com","displayName":"Ana","status":"active"}
"""

final class APIClientTests: XCTestCase {
    private func client(_ stub: StubTransport, base: String = "http://127.0.0.1:8080") -> APIClient {
        APIClient(baseURL: URL(string: base)!, transport: stub)
    }

    func testLoginDecodesTokenAccountAndFractionalInstant() async throws {
        let stub = StubTransport(body: """
        {"token":"t-123","expiresAt":"2026-11-01T12:00:00.123456Z","account":\(accountJSON)}
        """)
        let result = try await client(stub).login(email: "ana@example.com", password: "secret123")

        XCTAssertEqual(result.token, "t-123")
        XCTAssertEqual(result.account.displayName, "Ana")
        XCTAssertEqual(result.expiresAt.timeIntervalSince1970, 1_793_534_400.123, accuracy: 0.001)

        let recorded = await stub.requests
        let request = try XCTUnwrap(recorded.first)
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.url?.absoluteString, "http://127.0.0.1:8080/auth/login")
        XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
        let sent = try JSONSerialization.jsonObject(with: XCTUnwrap(request.httpBody)) as? [String: String]
        XCTAssertEqual(sent, ["email": "ana@example.com", "password": "secret123"])
    }

    func testLoginAcceptsInstantWithoutFractionalSeconds() async throws {
        let stub = StubTransport(body: """
        {"token":"t","expiresAt":"2026-11-01T12:00:00Z","account":\(accountJSON)}
        """)
        let result = try await client(stub).login(email: "a@b.co", password: "x")
        XCTAssertEqual(result.expiresAt.timeIntervalSince1970, 1_793_534_400, accuracy: 0.001)
    }

    func testParseInstantHandlesJacksonPrecisionsAndOffsets() throws {
        let base = 1_793_534_400.0
        XCTAssertEqual(try XCTUnwrap(APIClient.parseInstant("2026-11-01T12:00:00Z")).timeIntervalSince1970, base, accuracy: 1e-6)
        XCTAssertEqual(try XCTUnwrap(APIClient.parseInstant("2026-11-01T12:00:00.5Z")).timeIntervalSince1970, base + 0.5, accuracy: 1e-6)
        XCTAssertEqual(try XCTUnwrap(APIClient.parseInstant("2026-11-01T12:00:00.123456789Z")).timeIntervalSince1970,
                       base + 0.123456789, accuracy: 1e-6)
        XCTAssertEqual(try XCTUnwrap(APIClient.parseInstant("2026-11-01T07:00:00.250-05:00")).timeIntervalSince1970,
                       base + 0.25, accuracy: 1e-6)
        XCTAssertNil(APIClient.parseInstant("2026-11-01T12:00:00.Z"))
        XCTAssertNil(APIClient.parseInstant("yesterday"))
    }

    func testAuthenticatedCallSendsBearerToken() async throws {
        let stub = StubTransport(body: accountJSON)
        _ = try await client(stub).me(token: "abc")
        let recorded = await stub.requests
        let request = try XCTUnwrap(recorded.first)
        XCTAssertEqual(request.httpMethod, "GET")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer abc")
        XCTAssertNil(request.httpBody)
    }

    func testBaseURLPathPrefixIsKept() async throws {
        let stub = StubTransport(body: "[]")
        _ = try await client(stub, base: "https://api.ilecto.net/proho").listCommunities(token: "t")
        let recorded = await stub.requests
        let request = try XCTUnwrap(recorded.first)
        XCTAssertEqual(request.url?.absoluteString, "https://api.ilecto.net/proho/communities")
    }

    func testListCommunitiesIgnoresUnknownFields() async throws {
        let stub = StubTransport(body: """
        [{"id":"c1","key":"torres-del-parque","name":"Torres del Parque","kind":"ph_residential",
          "legalName":null,"taxId":null,"plan":null,"country":"CO","currency":"COP",
          "timezone":"America/Bogota","address":null,"somethingNew":42}]
        """)
        let communities = try await client(stub).listCommunities(token: "t")
        XCTAssertEqual(communities.map(\.key), ["torres-del-parque"])
        XCTAssertEqual(communities.first?.currency, "COP")
    }

    func testEmptyBodyIsFineForLogout() async throws {
        let stub = StubTransport(status: 204)
        try await client(stub).logout(token: "t")
        let count = await stub.requests.count
        XCTAssertEqual(count, 1)
    }

    func testServerErrorCodeIsSurfacedAndMapped() async {
        let stub = StubTransport(status: 409, body: #"{"error":"community_key_already_registered"}"#)
        do {
            _ = try await client(stub).createCommunity(token: "t", key: "dup", name: "Dup")
            XCTFail("expected an error")
        } catch let error as APIError {
            XCTAssertEqual(error, .server(status: 409, code: "community_key_already_registered"))
            XCTAssertEqual(error.messageKey, "error_community_key_already_registered")
        } catch {
            XCTFail("unexpected error \(error)")
        }
    }

    func testNonJSONErrorBodyBecomesUnknownError() async {
        let stub = StubTransport(status: 502, body: "<html>Bad Gateway</html>")
        do {
            _ = try await client(stub).me(token: "t")
            XCTFail("expected an error")
        } catch let error as APIError {
            XCTAssertEqual(error, .server(status: 502, code: "unknown_error"))
            XCTAssertEqual(error.messageKey, "error_unknown")
        } catch {
            XCTFail("unexpected error \(error)")
        }
    }

    func testMessageKeysMatchAndroidErrorMapping() {
        XCTAssertEqual(APIError.server(status: 401, code: "invalid_credentials").messageKey, "error_invalid_credentials")
        XCTAssertEqual(APIError.server(status: 409, code: "email_already_registered").messageKey, "error_email_already_registered")
        XCTAssertEqual(APIError.server(status: 400, code: "invalid_request").messageKey, "error_invalid_request")
        XCTAssertEqual(APIError.transport("offline").messageKey, "error_network")
    }
}
