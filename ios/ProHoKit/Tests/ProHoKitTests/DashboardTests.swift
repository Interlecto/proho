import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
import XCTest
@testable import ProHoKit

private let communityJSON = """
{"id":"c1","key":"los-robles","name":"Los Robles","legalName":null,"taxId":null,"kind":"ph_residential",
 "plan":null,"country":"CO","currency":"COP","timezone":"America/Bogota","address":"Calle 1 # 2-3"}
"""

/// The same fixtures as android/app/src/test/.../HomeParsingTest.kt.
final class DashboardTests: XCTestCase {
    private func client(_ stub: StubTransport) -> APIClient {
        APIClient(baseURL: URL(string: "http://127.0.0.1:8080")!, transport: stub)
    }

    func testAdministratorsHomeHasCounts() async throws {
        let stub = StubTransport(body: """
        {"community":\(communityJSON),"membershipId":"m1","personId":null,"roles":["administrator"],"units":[],
         "administration":{"buildings":1,"units":12,"people":30,"members":8,"suspendedMembers":1,"pendingInvitations":2}}
        """)
        let home = try await client(stub).home(token: "t", communityId: "c1")

        XCTAssertEqual(home.community.address, "Calle 1 # 2-3")
        XCTAssertNil(home.personId)
        XCTAssertTrue(home.isAdministrator)
        XCTAssertEqual(home.administration, AdministrationSummary(buildings: 1, units: 12, people: 30, members: 8,
                                                                  suspendedMembers: 1, pendingInvitations: 2))
        let recorded = await stub.requests
        XCTAssertEqual(recorded.first?.url?.absoluteString, "http://127.0.0.1:8080/communities/c1/home")
    }

    func testMembersHomeHasTheirUnitsAndNoCounts() async throws {
        let stub = StubTransport(body: """
        {"community":\(communityJSON),"membershipId":"m2","personId":"p1","roles":[],
         "units":[{"unitId":"u1","number":"P-7","kind":"parking","building":null,"roles":["tenant"]},
                  {"unitId":"u2","number":"201","kind":"apartment","building":"Torre 2","roles":["owner","resident"]}],
         "administration":null}
        """)
        let home = try await client(stub).home(token: "t", communityId: "c1")

        XCTAssertFalse(home.isAdministrator)
        XCTAssertNil(home.administration)
        XCTAssertEqual(home.units, [
            MyUnit(unitId: "u1", number: "P-7", kind: "parking", building: nil, roles: ["tenant"]),
            MyUnit(unitId: "u2", number: "201", kind: "apartment", building: "Torre 2", roles: ["owner", "resident"]),
        ])
    }

    func testHiddenPersonFieldsAreNil() async throws {
        let stub = StubTransport(body: """
        [{"id":"p1","communityId":"c1","kind":"natural","givenName":"Ana","surname":"Gómez","legalName":null,
          "idType":null,"idNumber":null,"email":null,"phone":null,"nameVisibility":"community",
          "emailVisibility":"privileged","phoneVisibility":"privileged","unitsVisibility":"privileged","archivedAt":null}]
        """)
        let people = try await client(stub).listPeople(token: "t", communityId: "c1")

        XCTAssertEqual(people.first?.displayName, "Ana Gómez")
        XCTAssertNil(people.first?.email)
    }

    func testRelationshipShareDecodesAsDecimal() async throws {
        let stub = StubTransport(body: """
        [{"id":"r1","unitId":"u1","personId":"p1","role":"owner","share":50.0000,"validFrom":"2026-01-01","validTo":null}]
        """)
        let relationships = try await client(stub).listUnitRelationships(token: "t", communityId: "c1", unitId: "u1")

        XCTAssertEqual(relationships.first?.share, Decimal(50))
        XCTAssertEqual(relationships.first?.validFrom, "2026-01-01")
    }

    func testProfileIsNilWhenNoPersonIsLinked() async throws {
        let stub = StubTransport(status: 404, body: #"{"error":"profile_not_linked"}"#)
        let profile = try await client(stub).profile(token: "t", communityId: "c1")
        XCTAssertNil(profile)
    }

    func testCreateUnitSendsNumberAndKind() async throws {
        let stub = StubTransport(status: 201, body: """
        {"id":"u9","communityId":"c1","buildingId":null,"number":"101","kind":"apartment","floor":null,
         "areaM2":null,"registryId":null,"archivedAt":null}
        """)
        _ = try await client(stub).createUnit(token: "t", communityId: "c1", number: "101", kind: "apartment")

        let recorded = await stub.requests
        let request = try XCTUnwrap(recorded.first)
        XCTAssertEqual(request.httpMethod, "POST")
        let sent = try JSONSerialization.jsonObject(with: XCTUnwrap(request.httpBody)) as? [String: String]
        XCTAssertEqual(sent, ["number": "101", "kind": "apartment"])
    }

    func testSectionsMatchWebAndAndroidOrder() {
        XCTAssertEqual(MemberSection.allCases, [.home, .units, .directory, .profile])
        XCTAssertEqual(MemberSection.directory.titleKey, "section_directory")
        XCTAssertTrue(MemberSection.units.canAdd(isAdministrator: true))
        XCTAssertFalse(MemberSection.units.canAdd(isAdministrator: false))
        XCTAssertFalse(MemberSection.home.canAdd(isAdministrator: true))
    }

    func testLabelKeysMatchAndroidResourceNames() {
        XCTAssertEqual(LabelKey.unitKind("parking"), "kind_parking")
        XCTAssertEqual(LabelKey.relationshipRole("household_member"), "relationship_household_member")
        XCTAssertEqual(LabelKey.assignedRole("council_member"), "role_council_member")
        XCTAssertNil(LabelKey.unitKind("castle"))
    }

    func testNewErrorCodesHaveMessages() {
        XCTAssertEqual(APIError.server(status: 409, code: "owner_share_exceeds_100").messageKey, "error_owner_share_exceeds_100")
        XCTAssertEqual(APIError.server(status: 403, code: "membership_not_active").messageKey, "error_membership_not_active")
    }
}
