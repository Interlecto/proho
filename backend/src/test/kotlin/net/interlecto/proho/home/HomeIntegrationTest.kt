package net.interlecto.proho.home

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.communities.CommunityResponse
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.communities.CreateInvitationRequest
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.MembershipStatus
import net.interlecto.proho.communities.Role
import net.interlecto.proho.communities.RoleAssignment
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
import net.interlecto.proho.residents.LinkPersonRequest
import net.interlecto.proho.residents.PersonResponse
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class HomeIntegrationTest {
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

    private fun <T : Any> call(
        token: String,
        method: HttpMethod,
        path: String,
        body: Any?,
        type: Class<T>,
    ): ResponseEntity<T> = rest.exchange(path, method, HttpEntity(body, HttpHeaders().apply { setBearerAuth(token) }), type)

    private fun login(tag: String): String {
        val email = "$tag@example.com"
        rest.postForEntity("/accounts", RegisterAccountRequest(email, "correct-horse-battery", "User $tag"), String::class.java)
        return rest.postForEntity("/auth/login", LoginRequest(email, "correct-horse-battery"), LoginResponse::class.java).body!!.token
    }

    private inner class Community {
        val tag = "home-${sequence.incrementAndGet()}"
        val adminToken = login(tag)
        val id: UUID =
            call(adminToken, HttpMethod.POST, "/communities", CreateCommunityRequest(tag, "Los Robles", kind = "ph_residential"), CommunityResponse::class.java)
                .body!!.id
        val base = "/communities/$id"
        private val members = AtomicInteger()

        fun home(token: String) = call(token, HttpMethod.GET, "$base/home", null, HomeResponse::class.java)

        fun building(name: String) =
            call(adminToken, HttpMethod.POST, "$base/buildings", CreateBuildingRequest(name), BuildingResponse::class.java).body!!

        fun unit(
            number: String,
            buildingId: UUID? = null,
        ) = call(adminToken, HttpMethod.POST, "$base/units", CreateUnitRequest(number, "apartment", buildingId), UnitResponse::class.java).body!!

        fun person(name: String) =
            call(adminToken, HttpMethod.POST, "$base/people", CreatePersonRequest("natural", name, "Gómez"), PersonResponse::class.java).body!!

        fun relate(
            unitId: UUID,
            personId: UUID,
            role: String,
            share: BigDecimal? = null,
            validFrom: LocalDate? = null,
            validTo: LocalDate? = null,
        ) = call(
            adminToken,
            HttpMethod.POST,
            "$base/units/$unitId/relationships",
            CreateUnitRelationshipRequest(personId, role, share, validFrom, validTo),
            String::class.java,
        ).also { assertThat(it.statusCode).isEqualTo(HttpStatus.CREATED) }

        /** A member's token and membership id, with the roles given. */
        fun member(vararg roles: Role): Pair<String, UUID> {
            val token = login("$tag-m${members.incrementAndGet()}")
            val accountId = call(token, HttpMethod.GET, "/me", null, AccountResponse::class.java).body!!.id
            val membershipId = Uuid7.generate()
            asCommunity(id) {
                jdbcAggregateOperations.insert(
                    Membership(
                        id = membershipId,
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
                roles.forEach {
                    jdbcAggregateOperations.insert(RoleAssignment(Uuid7.generate(), membershipId, id, it, LocalDate.now(), null, Instant.now()))
                }
            }
            return token to membershipId
        }

        fun link(
            membershipId: UUID,
            personId: UUID,
        ) = call(adminToken, HttpMethod.PUT, "$base/memberships/$membershipId/person", LinkPersonRequest(personId), String::class.java)
    }

    @Test
    fun `a new community's founder sees it, their role and empty counts`() {
        val c = Community()

        val home = c.home(c.adminToken).body!!

        assertThat(home.community.id).isEqualTo(c.id)
        assertThat(home.community.name).isEqualTo("Los Robles")
        assertThat(home.roles).containsExactly("administrator")
        assertThat(home.units).isEmpty()
        assertThat(home.administration).isEqualTo(AdministrationSummaryResponse(0, 0, 0, 1, 0, 0))
    }

    @Test
    fun `administrators see counts of what is active, not archived or ended`() {
        val c = Community()
        val building = c.building("Torre 1")
        c.unit("101", building.id)
        val archived = c.unit("102", building.id)
        call(c.adminToken, HttpMethod.POST, "${c.base}/units/${archived.id}/archive", null, String::class.java)
        c.person("Ana")
        c.member()
        val (_, suspended) = c.member()
        call(c.adminToken, HttpMethod.POST, "${c.base}/memberships/$suspended/suspend", null, String::class.java)
        call(c.adminToken, HttpMethod.POST, "${c.base}/invitations", CreateInvitationRequest("invited-${c.tag}@example.com"), String::class.java)

        val summary = c.home(c.adminToken).body!!.administration!!

        assertThat(summary).isEqualTo(AdministrationSummaryResponse(1, 1, 1, 2, 1, 1))
    }

    @Test
    fun `a member sees their own units today, with each role once, and no counts`() {
        val c = Community()
        val tower = c.building("Torre 2")
        val apartment = c.unit("201", tower.id)
        val parking = c.unit("P-7")
        val sold = c.unit("305", tower.id)
        val ana = c.person("Ana")
        c.relate(apartment.id, ana.id, "owner", share = BigDecimal("50"))
        c.relate(apartment.id, ana.id, "resident")
        c.relate(parking.id, ana.id, "tenant")
        c.relate(sold.id, ana.id, "owner", validFrom = LocalDate.now().minusYears(2), validTo = LocalDate.now().minusYears(1))
        val (token, membershipId) = c.member()
        c.link(membershipId, ana.id)

        val home = c.home(token).body!!

        assertThat(home.roles).isEmpty()
        assertThat(home.personId).isEqualTo(ana.id)
        assertThat(home.administration).isNull()
        assertThat(home.units).containsExactly(
            MyUnitResponse(parking.id, "P-7", "apartment", null, listOf("tenant")),
            MyUnitResponse(apartment.id, "201", "apartment", "Torre 2", listOf("owner", "resident")),
        )
    }

    @Test
    fun `other roles than administrator get no counts`() {
        val c = Community()
        val (token, _) = c.member(Role.TREASURER, Role.COUNCIL_MEMBER)

        val home = c.home(token).body!!

        assertThat(home.roles).containsExactlyInAnyOrder("treasurer", "council_member")
        assertThat(home.administration).isNull()
    }

    @Test
    fun `a member without a linked person has no units`() {
        val c = Community()
        val (token, _) = c.member()

        assertThat(c.home(token).body!!.units).isEmpty()
    }

    @Test
    fun `nobody outside the community, or suspended in it, gets its home`() {
        val c = Community()
        val other = Community()
        val (suspendedToken, suspended) = c.member()
        call(c.adminToken, HttpMethod.POST, "${c.base}/memberships/$suspended/suspend", null, String::class.java)

        assertThat(call(other.adminToken, HttpMethod.GET, "${c.base}/home", null, String::class.java).statusCode)
            .isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(call(suspendedToken, HttpMethod.GET, "${c.base}/home", null, String::class.java).statusCode)
            .isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(rest.getForEntity("${c.base}/home", String::class.java).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
