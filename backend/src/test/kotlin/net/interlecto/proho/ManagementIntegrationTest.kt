package net.interlecto.proho

import net.interlecto.proho.audit.AuditEventRepository
import net.interlecto.proho.communities.CommunityResponse
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.MembershipStatus
import net.interlecto.proho.communities.MyMembershipResponse
import net.interlecto.proho.communities.UpdateCommunityRequest
import net.interlecto.proho.identity.AccountResponse
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.properties.BuildingResponse
import net.interlecto.proho.properties.CreateBuildingRequest
import net.interlecto.proho.properties.CreateUnitRequest
import net.interlecto.proho.properties.UnitResponse
import net.interlecto.proho.residents.CreatePersonRequest
import net.interlecto.proho.residents.CreateUnitRelationshipRequest
import net.interlecto.proho.residents.EndUnitRelationshipRequest
import net.interlecto.proho.residents.PersonResponse
import net.interlecto.proho.residents.UnitRelationshipResponse
import net.interlecto.proho.shared.Uuid7
import net.interlecto.proho.tenancy.asCommunity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Editing, archiving and ending records, across communities, properties and residents. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ManagementIntegrationTest {
    companion object {
        private val sequence = AtomicInteger()

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @Autowired
    lateinit var auditEvents: AuditEventRepository

    private inner class Fixture {
        private val tag = "mgmt-${sequence.incrementAndGet()}"
        val token = login("$tag@example.com")
        val community =
            post("/communities", CreateCommunityRequest(tag, "Los Robles", kind = "ph_residential"), CommunityResponse::class.java)
        val base = "/communities/${community.body!!.id}"
        val id: UUID = community.body!!.id

        fun <T : Any> post(
            path: String,
            body: Any?,
            type: Class<T>,
        ): ResponseEntity<T> = exchange(HttpMethod.POST, path, body, type)

        fun <T : Any> put(
            path: String,
            body: Any,
            type: Class<T>,
        ): ResponseEntity<T> = exchange(HttpMethod.PUT, path, body, type)

        fun <T : Any> get(
            path: String,
            type: Class<T>,
        ): ResponseEntity<T> = exchange(HttpMethod.GET, path, null, type)

        fun <T : Any> exchange(
            method: HttpMethod,
            path: String,
            body: Any?,
            type: Class<T>,
        ): ResponseEntity<T> = rest.exchange(path, method, HttpEntity(body, HttpHeaders().apply { setBearerAuth(token) }), type)

        fun building(name: String = "Torre 1") = post("$base/buildings", CreateBuildingRequest(name), BuildingResponse::class.java).body!!

        fun unit(
            number: String = "101",
            buildingId: UUID? = null,
        ) = post("$base/units", CreateUnitRequest(number, "apartment", buildingId), UnitResponse::class.java).body!!

        fun person(
            idType: String? = null,
            idNumber: String? = null,
        ) = post(
            "$base/people",
            CreatePersonRequest("natural", "Ana", "Gómez", idType = idType, idNumber = idNumber),
            PersonResponse::class.java,
        ).body!!

        fun relationship(
            unitId: UUID,
            personId: UUID,
            role: String = "resident",
            share: BigDecimal? = null,
            validFrom: LocalDate? = null,
            validTo: LocalDate? = null,
        ) = post(
            "$base/units/$unitId/relationships",
            CreateUnitRelationshipRequest(personId, role, share, validFrom, validTo),
            String::class.java,
        )

        fun openRelationship(
            unitId: UUID,
            personId: UUID,
        ) = post(
            "$base/units/$unitId/relationships",
            CreateUnitRelationshipRequest(personId, "resident"),
            UnitRelationshipResponse::class.java,
        ).body!!

        /** A second account, an active member of this community with no assigned role. */
        fun plainMember(): Fixture2 {
            val memberToken = login("$tag-member@example.com")
            val headers = HttpHeaders().apply { setBearerAuth(memberToken) }
            val accountId =
                rest.exchange("/me", HttpMethod.GET, HttpEntity<Void>(headers), AccountResponse::class.java).body!!.id
            asCommunity(id) {
                jdbcAggregateOperations.insert(
                    Membership(
                        id = Uuid7.generate(),
                        accountId = accountId,
                        communityId = id,
                        personId = null,
                        status = MembershipStatus.ACTIVE,
                        invitedBy = null,
                        invitedAt = null,
                        joinedAt = Instant.now(),
                        endedAt = null,
                        createdAt = Instant.now(),
                    ),
                )
            }
            return Fixture2(memberToken)
        }

        private fun login(email: String): String {
            rest.postForEntity("/accounts", RegisterAccountRequest(email, "correct-horse-battery", "Test User"), String::class.java)
            return rest.postForEntity("/auth/login", LoginRequest(email, "correct-horse-battery"), LoginResponse::class.java)
                .body!!.token
        }
    }

    private inner class Fixture2(private val token: String) {
        fun status(
            method: HttpMethod,
            path: String,
            body: Any? = null,
        ): HttpStatus =
            HttpStatus.valueOf(
                rest.exchange(path, method, HttpEntity(body, HttpHeaders().apply { setBearerAuth(token) }), String::class.java)
                    .statusCode.value(),
            )
    }

    private fun assertError(
        response: ResponseEntity<String>,
        status: HttpStatus,
        code: String,
    ) {
        assertThat(response.statusCode).isEqualTo(status)
        assertThat(response.body).contains(code)
    }

    // Communities

    @Test
    fun `an administrator edits a community, but not its key or kind`() {
        val f = Fixture()

        val updated =
            f.put(f.base, UpdateCommunityRequest("Los Robles PH", "Edificio Los Robles", "900123456", "Calle 1"), CommunityResponse::class.java)

        assertThat(updated.statusCode).isEqualTo(HttpStatus.OK)
        val read = f.get(f.base, CommunityResponse::class.java).body!!
        assertThat(read.name).isEqualTo("Los Robles PH")
        assertThat(read.legalName).isEqualTo("Edificio Los Robles")
        assertThat(read.taxId).isEqualTo("900123456")
        assertThat(read.address).isEqualTo("Calle 1")
        assertThat(read.key).isEqualTo(f.community.body!!.key)
        assertThat(read.kind).isEqualTo("ph_residential")
    }

    @Test
    fun `a member reads a community and their roles, but can't edit it`() {
        val f = Fixture()
        val member = f.plainMember()

        assertThat(member.status(HttpMethod.GET, f.base)).isEqualTo(HttpStatus.OK)
        assertThat(member.status(HttpMethod.GET, "${f.base}/membership")).isEqualTo(HttpStatus.OK)
        assertThat(member.status(HttpMethod.PUT, f.base, UpdateCommunityRequest("Taken over"))).isEqualTo(HttpStatus.FORBIDDEN)

        assertThat(f.get("${f.base}/membership", MyMembershipResponse::class.java).body!!.roles).containsExactly("administrator")
    }

    @Test
    fun `an outsider can't read or edit a community`() {
        val f = Fixture()
        val other = Fixture()

        assertError(other.get(f.base, String::class.java), HttpStatus.FORBIDDEN, "membership_not_active")
        assertError(other.get("${f.base}/membership", String::class.java), HttpStatus.FORBIDDEN, "membership_not_active")
        assertError(other.put(f.base, UpdateCommunityRequest("Taken over"), String::class.java), HttpStatus.FORBIDDEN, "membership_not_active")
        assertThat(f.get(f.base, CommunityResponse::class.java).body!!.name).isEqualTo("Los Robles")
    }

    @Test
    fun `a community needs a real time zone`() {
        val f = Fixture()

        val response =
            f.post(
                "/communities",
                CreateCommunityRequest("bad-zone-${sequence.incrementAndGet()}", "X", kind = "ph_residential", timezone = "Mars/Olympus"),
                String::class.java,
            )

        assertError(response, HttpStatus.BAD_REQUEST, "invalid_request")
    }

    // Buildings

    @Test
    fun `an administrator renames a building and reads it back`() {
        val f = Fixture()
        val building = f.building()

        f.put("${f.base}/buildings/${building.id}", CreateBuildingRequest("Torre Norte"), BuildingResponse::class.java)

        assertThat(f.get("${f.base}/buildings/${building.id}", BuildingResponse::class.java).body!!.name).isEqualTo("Torre Norte")
    }

    @Test
    fun `a building with active units can't be archived until they are`() {
        val f = Fixture()
        val building = f.building()
        val unit = f.unit(buildingId = building.id)

        assertError(f.post("${f.base}/buildings/${building.id}/archive", null, String::class.java), HttpStatus.CONFLICT, "building_in_use")

        f.post("${f.base}/units/${unit.id}/archive", null, UnitResponse::class.java)
        val archived = f.post("${f.base}/buildings/${building.id}/archive", null, BuildingResponse::class.java)

        assertThat(archived.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(archived.body!!.archivedAt).isNotNull()
    }

    @Test
    fun `an archived building leaves the list until it's restored`() {
        val f = Fixture()
        val building = f.building()
        f.post("${f.base}/buildings/${building.id}/archive", null, BuildingResponse::class.java)

        assertThat(f.get("${f.base}/buildings", Array<BuildingResponse>::class.java).body).isEmpty()
        assertThat(f.get("${f.base}/buildings?includeArchived=true", Array<BuildingResponse>::class.java).body).hasSize(1)

        val restored = f.post("${f.base}/buildings/${building.id}/restore", null, BuildingResponse::class.java)

        assertThat(restored.body!!.archivedAt).isNull()
        assertThat(f.get("${f.base}/buildings", Array<BuildingResponse>::class.java).body).hasSize(1)
    }

    @Test
    fun `an archived building can't be edited or given units`() {
        val f = Fixture()
        val building = f.building()
        f.post("${f.base}/buildings/${building.id}/archive", null, BuildingResponse::class.java)

        assertError(
            f.put("${f.base}/buildings/${building.id}", CreateBuildingRequest("X"), String::class.java),
            HttpStatus.CONFLICT,
            "record_archived",
        )
        assertError(
            f.post("${f.base}/units", CreateUnitRequest("101", "apartment", building.id), String::class.java),
            HttpStatus.CONFLICT,
            "record_archived",
        )
    }

    @Test
    fun `archiving twice records one audit event`() {
        val f = Fixture()
        val building = f.building()

        f.post("${f.base}/buildings/${building.id}/archive", null, BuildingResponse::class.java)
        val second = f.post("${f.base}/buildings/${building.id}/archive", null, BuildingResponse::class.java)

        assertThat(second.statusCode).isEqualTo(HttpStatus.OK)
        val archived =
            asCommunity(f.id) { auditEvents.findAll().filter { it.entityId == building.id && it.action == "archived" } }
        assertThat(archived).hasSize(1)
    }

    // Units

    @Test
    fun `an administrator edits a unit and moves it between buildings`() {
        val f = Fixture()
        val first = f.building("Torre 1")
        val second = f.building("Torre 2")
        val unit = f.unit(buildingId = first.id)

        val request = CreateUnitRequest("102", "office", second.id, "1", BigDecimal("55.50"), "50N-123")
        f.put("${f.base}/units/${unit.id}", request, UnitResponse::class.java)

        val read = f.get("${f.base}/units/${unit.id}", UnitResponse::class.java).body!!
        assertThat(read.number).isEqualTo("102")
        assertThat(read.kind).isEqualTo("office")
        assertThat(read.buildingId).isEqualTo(second.id)
        assertThat(read.floor).isEqualTo("1")
        assertThat(read.areaM2).isEqualByComparingTo("55.50")
        assertThat(read.registryId).isEqualTo("50N-123")
    }

    @Test
    fun `a unit can keep its number but not take another's, even an archived one's`() {
        val f = Fixture()
        val archived = f.unit("100")
        f.post("${f.base}/units/${archived.id}/archive", null, UnitResponse::class.java)
        f.unit("101")
        val unit = f.unit("102")

        val same = f.put("${f.base}/units/${unit.id}", CreateUnitRequest("102", "house"), String::class.java)
        assertThat(same.statusCode).isEqualTo(HttpStatus.OK)

        val path = "${f.base}/units/${unit.id}"
        assertError(f.put(path, CreateUnitRequest("101", "house"), String::class.java), HttpStatus.CONFLICT, "unit_number_already_registered")
        assertError(f.put(path, CreateUnitRequest("100", "house"), String::class.java), HttpStatus.CONFLICT, "unit_number_already_registered")
    }

    @Test
    fun `a unit's area can't be negative or overflow`() {
        val f = Fixture()

        val negative = CreateUnitRequest("1", "apartment", areaM2 = BigDecimal("-1"))
        val huge = CreateUnitRequest("1", "apartment", areaM2 = BigDecimal("123456789"))

        assertError(f.post("${f.base}/units", negative, String::class.java), HttpStatus.BAD_REQUEST, "invalid_request")
        assertError(f.post("${f.base}/units", huge, String::class.java), HttpStatus.BAD_REQUEST, "invalid_request")
    }

    @Test
    fun `a unit can't move into another community's building`() {
        val f = Fixture()
        val other = Fixture()
        val unit = f.unit()

        val response =
            f.put("${f.base}/units/${unit.id}", CreateUnitRequest("101", "apartment", other.building().id), String::class.java)

        assertError(response, HttpStatus.BAD_REQUEST, "not_found_in_community")
    }

    @Test
    fun `a unit with an open relationship can't be archived until it ends`() {
        val f = Fixture()
        val unit = f.unit()
        val relationship = f.openRelationship(unit.id, f.person().id)

        assertError(f.post("${f.base}/units/${unit.id}/archive", null, String::class.java), HttpStatus.CONFLICT, "unit_in_use")

        f.post("${f.base}/units/${unit.id}/relationships/${relationship.id}/end", null, UnitRelationshipResponse::class.java)
        val archived = f.post("${f.base}/units/${unit.id}/archive", null, UnitResponse::class.java)

        assertThat(archived.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(archived.body!!.archivedAt).isNotNull()
    }

    @Test
    fun `a unit can't be restored into an archived building`() {
        val f = Fixture()
        val building = f.building()
        val unit = f.unit(buildingId = building.id)
        f.post("${f.base}/units/${unit.id}/archive", null, UnitResponse::class.java)
        f.post("${f.base}/buildings/${building.id}/archive", null, BuildingResponse::class.java)

        assertError(f.post("${f.base}/units/${unit.id}/restore", null, String::class.java), HttpStatus.CONFLICT, "record_archived")

        f.post("${f.base}/buildings/${building.id}/restore", null, BuildingResponse::class.java)
        assertThat(f.post("${f.base}/units/${unit.id}/restore", null, UnitResponse::class.java).body!!.archivedAt).isNull()
    }

    // People

    @Test
    fun `an administrator edits a person, and the audit log doesn't record who`() {
        val f = Fixture()
        val person = f.person()

        val request = CreatePersonRequest("natural", "Ana María", "Gómez Ruiz", idType = "CC", idNumber = "52123456")
        f.put("${f.base}/people/${person.id}", request, PersonResponse::class.java)

        val read = f.get("${f.base}/people/${person.id}", PersonResponse::class.java).body!!
        assertThat(read.givenName).isEqualTo("Ana María")
        assertThat(read.idNumber).isEqualTo("52123456")
        val events = asCommunity(f.id) { auditEvents.findAll().filter { it.entityId == person.id } }
        assertThat(events.map { it.action }).contains("updated")
        assertThat(events.mapNotNull { it.summary }).isEmpty()
    }

    @Test
    fun `a person keeps their own document but can't take another's`() {
        val f = Fixture()
        f.person("CC", "111")
        val person = f.person("CC", "222")
        val path = "${f.base}/people/${person.id}"

        val same = CreatePersonRequest("natural", "Ana B", "Gómez", idType = "CC", idNumber = "222")
        assertThat(f.put(path, same, String::class.java).statusCode).isEqualTo(HttpStatus.OK)

        val taken = CreatePersonRequest("natural", "Ana", "Gómez", idType = "CC", idNumber = "111")
        assertError(f.put(path, taken, String::class.java), HttpStatus.CONFLICT, "person_id_already_registered")
    }

    @Test
    fun `an identity document needs a supported type and a number together`() {
        val f = Fixture()
        val bad =
            listOf(
                CreatePersonRequest("natural", "A", "B", idType = "XX", idNumber = "1"),
                CreatePersonRequest("natural", "A", "B", idNumber = "1"),
                CreatePersonRequest("natural", "A", "B", idType = "CC"),
            )

        bad.forEach { assertError(f.post("${f.base}/people", it, String::class.java), HttpStatus.BAD_REQUEST, "invalid_request") }
    }

    @Test
    fun `a person with an open relationship can't be archived, and an archived one can't be linked`() {
        val f = Fixture()
        val unit = f.unit()
        val person = f.person()
        val relationship = f.openRelationship(unit.id, person.id)

        assertError(f.post("${f.base}/people/${person.id}/archive", null, String::class.java), HttpStatus.CONFLICT, "person_in_use")

        f.post("${f.base}/units/${unit.id}/relationships/${relationship.id}/end", null, UnitRelationshipResponse::class.java)
        f.post("${f.base}/people/${person.id}/archive", null, PersonResponse::class.java)

        assertThat(f.get("${f.base}/people", Array<PersonResponse>::class.java).body).isEmpty()
        assertError(f.relationship(unit.id, person.id), HttpStatus.CONFLICT, "record_archived")
        val history = f.get("${f.base}/people/${person.id}/relationships", Array<UnitRelationshipResponse>::class.java).body!!
        assertThat(history.map { it.id }).containsExactly(relationship.id)

        f.post("${f.base}/people/${person.id}/restore", null, PersonResponse::class.java)
        assertThat(f.relationship(unit.id, person.id).statusCode).isEqualTo(HttpStatus.CREATED)
    }

    // Relationships

    @Test
    fun `ending a relationship keeps it as history, and it only ends once`() {
        val f = Fixture()
        val unit = f.unit()
        val relationship = f.openRelationship(unit.id, f.person().id)
        val path = "${f.base}/units/${unit.id}/relationships/${relationship.id}/end"

        val ended = f.post(path, null, UnitRelationshipResponse::class.java)

        assertThat(ended.body!!.validTo).isNotNull()
        assertThat(f.get("${f.base}/units/${unit.id}/relationships", Array<UnitRelationshipResponse>::class.java).body).hasSize(1)
        assertError(f.post(path, null, String::class.java), HttpStatus.CONFLICT, "relationship_already_ended")
    }

    @Test
    fun `a relationship can't end before it starts, or later than an end already set`() {
        val f = Fixture()
        val unit = f.unit()
        val person = f.person()
        val from = LocalDate.now().minusMonths(1)
        val to = LocalDate.now().plusMonths(1)
        val relationship =
            f.post(
                "${f.base}/units/${unit.id}/relationships",
                CreateUnitRelationshipRequest(person.id, "tenant", validFrom = from, validTo = to),
                UnitRelationshipResponse::class.java,
            ).body!!
        val path = "${f.base}/units/${unit.id}/relationships/${relationship.id}/end"

        assertError(f.post(path, EndUnitRelationshipRequest(from.minusDays(1)), String::class.java), HttpStatus.BAD_REQUEST, "invalid_request")
        assertError(f.post(path, EndUnitRelationshipRequest(to.plusDays(1)), String::class.java), HttpStatus.BAD_REQUEST, "invalid_request")

        val earlier = to.minusDays(10)
        assertThat(f.post(path, EndUnitRelationshipRequest(earlier), UnitRelationshipResponse::class.java).body!!.validTo).isEqualTo(earlier)
    }

    @Test
    fun `a relationship is only reachable through its own unit`() {
        val f = Fixture()
        val unit = f.unit("101")
        val otherUnit = f.unit("102")
        val relationship = f.openRelationship(unit.id, f.person().id)

        val response = f.post("${f.base}/units/${otherUnit.id}/relationships/${relationship.id}/end", null, String::class.java)

        assertError(response, HttpStatus.NOT_FOUND, "not_found_in_community")
    }

    @Test
    fun `a period can't end before it starts`() {
        val f = Fixture()

        val response =
            f.relationship(f.unit().id, f.person().id, validFrom = LocalDate.of(2026, 2, 1), validTo = LocalDate.of(2026, 1, 1))

        assertError(response, HttpStatus.BAD_REQUEST, "invalid_request")
    }

    @Test
    fun `an owner share has to be between 0 and 100`() {
        val f = Fixture()
        val unit = f.unit()
        val person = f.person()

        assertError(f.relationship(unit.id, person.id, "owner", BigDecimal("-1")), HttpStatus.BAD_REQUEST, "invalid_request")
        assertError(f.relationship(unit.id, person.id, "owner", BigDecimal("100.01")), HttpStatus.BAD_REQUEST, "invalid_request")
    }

    @Test
    fun `ownership can pass from one owner to the next`() {
        val f = Fixture()
        val unit = f.unit()
        val seller = f.person()
        val buyer = f.person()
        val sold = LocalDate.of(2026, 6, 30)

        val first = f.relationship(unit.id, seller.id, "owner", BigDecimal("100"), LocalDate.of(2020, 1, 1), sold)
        val second = f.relationship(unit.id, buyer.id, "owner", BigDecimal("100"), sold.plusDays(1))

        assertThat(first.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(second.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `owner shares can't exceed 100 on any day, including with an end date still ahead`() {
        val f = Fixture()
        val unit = f.unit()
        val future = LocalDate.now().plusYears(1)
        f.relationship(unit.id, f.person().id, "owner", BigDecimal("100"), LocalDate.of(2020, 1, 1), future)

        val overlapping = f.relationship(unit.id, f.person().id, "owner", BigDecimal("1"))
        assertError(overlapping, HttpStatus.CONFLICT, "owner_share_exceeds_100")

        val starting = f.relationship(unit.id, f.person().id, "owner", BigDecimal("100"), future.plusDays(1))
        assertThat(starting.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `shares in separate periods don't add up`() {
        val f = Fixture()
        val unit = f.unit()
        f.relationship(unit.id, f.person().id, "owner", BigDecimal("50"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
        f.relationship(unit.id, f.person().id, "owner", BigDecimal("50"), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30))

        val acrossBoth = f.relationship(unit.id, f.person().id, "owner", BigDecimal("50"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))
        assertThat(acrossBoth.statusCode).isEqualTo(HttpStatus.CREATED)

        val oneTooMany = f.relationship(unit.id, f.person().id, "owner", BigDecimal("1"), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1))
        assertError(oneTooMany, HttpStatus.CONFLICT, "owner_share_exceeds_100")
    }

    // Authorization and isolation

    @Test
    fun `a member with no role can read records but not change them`() {
        val f = Fixture()
        val building = f.building()
        val unit = f.unit(buildingId = building.id)
        val person = f.person()
        val relationship = f.openRelationship(unit.id, person.id)
        val member = f.plainMember()

        listOf(
            "${f.base}/buildings/${building.id}",
            "${f.base}/units/${unit.id}",
            "${f.base}/units/${unit.id}/relationships",
            "${f.base}/people",
        ).forEach { assertThat(member.status(HttpMethod.GET, it)).isEqualTo(HttpStatus.OK) }

        listOf(
            "${f.base}/buildings?includeArchived=true",
            "${f.base}/units?includeArchived=true",
            "${f.base}/people?includeArchived=true",
        ).forEach { assertThat(member.status(HttpMethod.GET, it)).isEqualTo(HttpStatus.FORBIDDEN) }

        val writes =
            listOf(
                Triple(HttpMethod.PUT, "${f.base}/buildings/${building.id}", CreateBuildingRequest("X")),
                Triple(HttpMethod.POST, "${f.base}/buildings/${building.id}/archive", null),
                Triple(HttpMethod.POST, "${f.base}/buildings/${building.id}/restore", null),
                Triple(HttpMethod.PUT, "${f.base}/units/${unit.id}", CreateUnitRequest("9", "house")),
                Triple(HttpMethod.POST, "${f.base}/units/${unit.id}/archive", null),
                Triple(HttpMethod.POST, "${f.base}/units/${unit.id}/restore", null),
                Triple(HttpMethod.PUT, "${f.base}/people/${person.id}", CreatePersonRequest("natural", "X", "Y")),
                Triple(HttpMethod.POST, "${f.base}/people/${person.id}/archive", null),
                Triple(HttpMethod.POST, "${f.base}/people/${person.id}/restore", null),
                Triple(HttpMethod.POST, "${f.base}/units/${unit.id}/relationships/${relationship.id}/end", null),
            )
        writes.forEach { (method, path, body) -> assertThat(member.status(method, path, body)).isEqualTo(HttpStatus.FORBIDDEN) }
    }

    @Test
    fun `another community's records aren't found through your own`() {
        val f = Fixture()
        val other = Fixture()
        val building = other.building()
        val unit = other.unit(buildingId = building.id)
        val person = other.person()
        val relationship = other.openRelationship(unit.id, person.id)

        val paths =
            listOf(
                "${f.base}/buildings/${building.id}",
                "${f.base}/units/${unit.id}",
                "${f.base}/people/${person.id}",
                "${f.base}/people/${person.id}/relationships",
            )
        paths.forEach { assertError(f.get(it, String::class.java), HttpStatus.NOT_FOUND, "not_found_in_community") }

        val writes =
            listOf(
                f.put("${f.base}/buildings/${building.id}", CreateBuildingRequest("X"), String::class.java),
                f.post("${f.base}/buildings/${building.id}/archive", null, String::class.java),
                f.put("${f.base}/units/${unit.id}", CreateUnitRequest("9", "house"), String::class.java),
                f.post("${f.base}/units/${unit.id}/archive", null, String::class.java),
                f.put("${f.base}/people/${person.id}", CreatePersonRequest("natural", "X", "Y"), String::class.java),
                f.post("${f.base}/people/${person.id}/archive", null, String::class.java),
                f.post("${f.base}/units/${unit.id}/relationships/${relationship.id}/end", null, String::class.java),
            )
        writes.forEach { assertError(it, HttpStatus.NOT_FOUND, "not_found_in_community") }

        assertThat(other.get("${other.base}/buildings/${building.id}", BuildingResponse::class.java).body!!.name).isEqualTo("Torre 1")
        assertThat(other.get("${other.base}/units/${unit.id}", UnitResponse::class.java).body!!.number).isEqualTo("101")
    }
}
