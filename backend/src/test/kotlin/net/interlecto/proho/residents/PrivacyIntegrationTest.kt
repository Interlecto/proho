package net.interlecto.proho.residents

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.audit.AuditEventRepository
import net.interlecto.proho.communities.CommunityResponse
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.communities.MemberResponse
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.MembershipStatus
import net.interlecto.proho.communities.Role
import net.interlecto.proho.communities.RoleAssignment
import net.interlecto.proho.identity.AccountResponse
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.identity.UpdateAccountRequest
import net.interlecto.proho.properties.CreateUnitRequest
import net.interlecto.proho.properties.UnitResponse
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
class PrivacyIntegrationTest {
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

    private inner class Member(val token: String, val membershipId: UUID, val base: String) {
        fun people() = call(token, HttpMethod.GET, "$base/people", null, Array<PersonResponse>::class.java).body!!.toList()

        fun person(id: UUID) = call(token, HttpMethod.GET, "$base/people/$id", null, String::class.java)

        fun personBody(id: UUID) = call(token, HttpMethod.GET, "$base/people/$id", null, PersonResponse::class.java).body!!

        fun unitRelationships(unitId: UUID) =
            call(token, HttpMethod.GET, "$base/units/$unitId/relationships", null, Array<UnitRelationshipResponse>::class.java)
                .body!!.toList()

        fun personRelationships(id: UUID) =
            call(token, HttpMethod.GET, "$base/people/$id/relationships", null, Array<UnitRelationshipResponse>::class.java).body!!.toList()

        fun profile() = call(token, HttpMethod.GET, "$base/profile", null, PersonResponse::class.java)

        fun share(
            name: String = VISIBILITY_COMMUNITY,
            email: String = VISIBILITY_PRIVILEGED,
            phone: String = VISIBILITY_PRIVILEGED,
            units: String = VISIBILITY_PRIVILEGED,
            emailAddress: String? = null,
            phoneNumber: String? = null,
        ) = call(
            token,
            HttpMethod.PUT,
            "$base/profile",
            UpdateProfileRequest(emailAddress, phoneNumber, name, email, phone, units),
            String::class.java,
        )
    }

    private inner class Community {
        private val tag = "privacy-${sequence.incrementAndGet()}"
        val adminToken = login(tag)
        val id: UUID =
            call(adminToken, HttpMethod.POST, "/communities", CreateCommunityRequest(tag, "Los Robles", kind = "ph_residential"), CommunityResponse::class.java)
                .body!!.id
        val base = "/communities/$id"
        private val members = AtomicInteger()

        fun member(vararg roles: Role): Member {
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
                    jdbcAggregateOperations.insert(
                        RoleAssignment(Uuid7.generate(), membershipId, id, it, LocalDate.now(), null, Instant.now()),
                    )
                }
            }
            return Member(token, membershipId, base)
        }

        fun person(
            name: String = "Ana",
            idNumber: String? = "${sequence.incrementAndGet()}",
            email: String? = "ana@example.com",
            phone: String? = "3001234567",
        ) = call(
            adminToken,
            HttpMethod.POST,
            "$base/people",
            CreatePersonRequest("natural", name, "Gómez", idType = idNumber?.let { "CC" }, idNumber = idNumber, email = email, phone = phone),
            PersonResponse::class.java,
        ).body!!

        fun unit(number: String = "101") =
            call(adminToken, HttpMethod.POST, "$base/units", CreateUnitRequest(number, "apartment"), UnitResponse::class.java).body!!

        fun link(
            member: Member,
            personId: UUID?,
        ) = call(adminToken, HttpMethod.PUT, "$base/memberships/${member.membershipId}/person", LinkPersonRequest(personId), String::class.java)

        fun relate(
            unitId: UUID,
            personId: UUID,
            role: String = "resident",
            share: BigDecimal? = null,
            validFrom: LocalDate? = null,
            validTo: LocalDate? = null,
        ) = call(
            adminToken,
            HttpMethod.POST,
            "$base/units/$unitId/relationships",
            CreateUnitRelationshipRequest(personId, role, share, validFrom, validTo),
            UnitRelationshipResponse::class.java,
        ).body!!

        /** A member linked to a new person record, as a resident of [unitId] if given. */
        fun resident(
            name: String,
            unitId: UUID? = null,
        ): Pair<Member, PersonResponse> {
            val person = person(name)
            val member = member()
            link(member, person.id)
            unitId?.let { relate(it, person.id) }
            return member to person
        }
    }

    private fun assertLooksMissing(response: ResponseEntity<String>) {
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).contains("not_found_in_community")
    }

    // Defaults

    @Test
    fun `a member sees nobody by default, not even that they exist`() {
        val c = Community()
        val person = c.person()
        val member = c.member()

        assertThat(member.people()).isEmpty()
        assertLooksMissing(member.person(person.id))
        assertLooksMissing(call(member.token, HttpMethod.GET, "${c.base}/people/${person.id}/relationships", null, String::class.java))
        assertLooksMissing(member.person(UUID.randomUUID()))
    }

    @Test
    fun `a member can't tell who lives in a unit by default`() {
        val c = Community()
        val unit = c.unit()
        c.relate(unit.id, c.person().id)

        assertThat(c.member().unitRelationships(unit.id)).isEmpty()
    }

    // Choosing

    @Test
    fun `sharing a name lists the person, and nothing else about them`() {
        val c = Community()
        val unit = c.unit()
        val (ana, person) = c.resident("Ana", unit.id)
        ana.share(emailAddress = "ana@example.com", phoneNumber = "300")
        val other = c.member()

        val seen = other.people().single()

        assertThat(seen.givenName).isEqualTo("Ana")
        assertThat(seen.email).isNull()
        assertThat(seen.phone).isNull()
        assertThat(seen.idNumber).isNull()
        assertThat(seen.idType).isNull()
        assertThat(other.unitRelationships(unit.id)).isEmpty()
        assertThat(other.personRelationships(person.id)).isEmpty()
    }

    @Test
    fun `each field is shared on its own`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")
        val other = c.member()

        ana.share(email = VISIBILITY_COMMUNITY, emailAddress = "ana@example.com", phoneNumber = "300")
        assertThat(other.personBody(person.id).email).isEqualTo("ana@example.com")
        assertThat(other.personBody(person.id).phone).isNull()

        ana.share(phone = VISIBILITY_COMMUNITY, emailAddress = "ana@example.com", phoneNumber = "300")
        assertThat(other.personBody(person.id).email).isNull()
        assertThat(other.personBody(person.id).phone).isEqualTo("300")
    }

    @Test
    fun `contact details stay hidden unless the name is shared too`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")
        ana.share(name = VISIBILITY_PRIVILEGED, email = VISIBILITY_COMMUNITY, units = VISIBILITY_COMMUNITY, emailAddress = "ana@example.com")

        assertLooksMissing(c.member().person(person.id))
    }

    @Test
    fun `sharing units shows where someone lives today, but not their past or their share`() {
        val c = Community()
        val current = c.unit("101")
        val past = c.unit("102")
        val (ana, person) = c.resident("Ana")
        c.relate(current.id, person.id, "owner", BigDecimal("50"))
        c.relate(past.id, person.id, validFrom = LocalDate.of(2020, 1, 1), validTo = LocalDate.of(2021, 1, 1))
        ana.share(units = VISIBILITY_COMMUNITY)
        val other = c.member()

        val seen = other.personRelationships(person.id).single()

        assertThat(seen.unitId).isEqualTo(current.id)
        assertThat(seen.share).isNull()
        assertThat(other.unitRelationships(past.id)).isEmpty()
    }

    @Test
    fun `withdrawing a choice takes effect immediately`() {
        val c = Community()
        val (ana, _) = c.resident("Ana")
        val other = c.member()
        ana.share()
        assertThat(other.people()).hasSize(1)

        ana.share(name = VISIBILITY_PRIVILEGED)

        assertThat(other.people()).isEmpty()
    }

    @Test
    fun `an unknown visibility is rejected`() {
        val c = Community()
        val (ana, _) = c.resident("Ana")

        assertThat(ana.share(name = "public").statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `an administrator can't share someone else's details`() {
        val c = Community()
        val (_, person) = c.resident("Ana")
        val request =
            mapOf(
                "kind" to "natural",
                "givenName" to "Ana",
                "surname" to "Gómez",
                "nameVisibility" to VISIBILITY_COMMUNITY,
                "emailVisibility" to VISIBILITY_COMMUNITY,
            )

        call(c.adminToken, HttpMethod.PUT, "${c.base}/people/${person.id}", request, String::class.java)

        assertThat(c.member().people()).isEmpty()
    }

    // People who share a unit

    @Test
    fun `people on the same unit see each other's names and roles, but not contact details`() {
        val c = Community()
        val unit = c.unit()
        val (_, owner) = c.resident("Olga", unit.id)
        val (tenant, _) = c.resident("Tito", unit.id)

        val seen = tenant.people().map { it.givenName }
        val relationship = tenant.unitRelationships(unit.id)

        assertThat(seen).containsExactlyInAnyOrder("Olga", "Tito")
        assertThat(relationship).hasSize(2)
        val body = tenant.personBody(owner.id)
        assertThat(body.email).isNull()
        assertThat(body.idNumber).isNull()
    }

    @Test
    fun `leaving a unit ends that view`() {
        val c = Community()
        val unit = c.unit()
        val (_, owner) = c.resident("Olga", unit.id)
        val person = c.person("Tito")
        val tenant = c.member()
        c.link(tenant, person.id)
        c.relate(unit.id, person.id, validFrom = LocalDate.of(2020, 1, 1), validTo = LocalDate.of(2021, 1, 1))

        assertLooksMissing(tenant.person(owner.id))
        assertThat(tenant.unitRelationships(unit.id).map { it.personId }).containsExactly(person.id)
    }

    // Privileged roles

    @Test
    fun `privileged roles see contact details, and only some see identity documents`() {
        val c = Community()
        val person = c.person()

        listOf(Role.ADMINISTRATOR, Role.ACCOUNTANT, Role.AUDITOR).forEach {
            val seen = c.member(it).personBody(person.id)
            assertThat(seen.email).isEqualTo("ana@example.com")
            assertThat(seen.idNumber).isEqualTo(person.idNumber)
        }
        listOf(Role.TREASURER, Role.COUNCIL_MEMBER).forEach {
            val seen = c.member(it).personBody(person.id)
            assertThat(seen.email).isEqualTo("ana@example.com")
            assertThat(seen.phone).isEqualTo("3001234567")
            assertThat(seen.idNumber).isNull()
            assertThat(seen.idType).isNull()
        }
    }

    @Test
    fun `moderators and staff are ordinary members here`() {
        val c = Community()
        c.person()

        assertThat(c.member(Role.MODERATOR).people()).isEmpty()
        assertThat(c.member(Role.STAFF).people()).isEmpty()
    }

    @Test
    fun `privileged roles see history and shares`() {
        val c = Community()
        val unit = c.unit()
        val person = c.person()
        c.relate(unit.id, person.id, "owner", BigDecimal("50"), LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))

        val seen = c.member(Role.TREASURER).unitRelationships(unit.id).single()

        assertThat(seen.share).isEqualByComparingTo("50")
    }

    // A person's own record

    @Test
    fun `a member reads their whole record, and changes only contact and sharing`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")

        assertThat(ana.profile().body!!.idNumber).isEqualTo(person.idNumber)
        assertThat(ana.personBody(person.id).idNumber).isEqualTo(person.idNumber)

        ana.share(emailAddress = "nueva@example.com", phoneNumber = "311")

        val after = ana.profile().body!!
        assertThat(after.email).isEqualTo("nueva@example.com")
        assertThat(after.phone).isEqualTo("311")
        assertThat(after.givenName).isEqualTo("Ana")
        assertThat(after.idNumber).isEqualTo(person.idNumber)
        assertThat(after.nameVisibility).isEqualTo(VISIBILITY_COMMUNITY)
    }

    @Test
    fun `a bad email is rejected`() {
        val c = Community()
        val (ana, _) = c.resident("Ana")

        assertThat(ana.share(emailAddress = "not-an-email").statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `a member sees their own past relationships`() {
        val c = Community()
        val unit = c.unit()
        val (ana, person) = c.resident("Ana")
        c.relate(unit.id, person.id, "owner", BigDecimal("50"), LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))

        val mine = ana.personRelationships(person.id).single()

        assertThat(mine.share).isEqualByComparingTo("50")
    }

    @Test
    fun `a member with no linked person has no profile`() {
        val c = Community()
        val member = c.member()

        val read = call(member.token, HttpMethod.GET, "${c.base}/profile", null, String::class.java)
        val write = member.share()

        assertThat(read.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(read.body).contains("profile_not_linked")
        assertThat(write.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `the audit log records a profile change without its contents`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")

        ana.share(emailAddress = "secreto@example.com")

        val event = asCommunity(c.id) { auditEvents.findAll().single { it.entityId == person.id && it.action == "profile_updated" } }
        assertThat(event.summary).isNull()
    }

    // Linking

    @Test
    fun `only an administrator links a member to a person`() {
        val c = Community()
        val person = c.person()
        val member = c.member()
        val other = c.member(Role.TREASURER)

        val forbidden =
            call(other.token, HttpMethod.PUT, "${c.base}/memberships/${member.membershipId}/person", LinkPersonRequest(person.id), String::class.java)
        assertThat(forbidden.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val selfLink =
            call(member.token, HttpMethod.PUT, "${c.base}/memberships/${member.membershipId}/person", LinkPersonRequest(person.id), String::class.java)
        assertThat(selfLink.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(member.people()).isEmpty()
    }

    @Test
    fun `a person belongs to one member at a time`() {
        val c = Community()
        val person = c.person()
        val first = c.member()
        val second = c.member()
        assertThat(c.link(first, person.id).statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val taken = c.link(second, person.id)
        assertThat(taken.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(taken.body).contains("person_already_linked")

        c.link(first, null)
        assertThat(c.link(second, person.id).statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(second.profile().body!!.id).isEqualTo(person.id)
    }

    @Test
    fun `unlinking takes the record away`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")

        c.link(ana, null)

        assertLooksMissing(ana.person(person.id))
    }

    @Test
    fun `a linked person can't be archived, and an archived one can't be linked`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")

        val inUse = call(c.adminToken, HttpMethod.POST, "${c.base}/people/${person.id}/archive", null, String::class.java)
        assertThat(inUse.statusCode).isEqualTo(HttpStatus.CONFLICT)

        c.link(ana, null)
        call(c.adminToken, HttpMethod.POST, "${c.base}/people/${person.id}/archive", null, String::class.java)
        assertThat(c.link(ana, person.id).statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `a membership can't be linked to another community's person or from another community`() {
        val c = Community()
        val other = Community()
        val member = c.member()

        val foreignPerson = c.link(member, other.person().id)
        assertThat(foreignPerson.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(foreignPerson.body).contains("not_found_in_community")

        val foreignMembership = other.link(member, other.person().id)
        assertThat(foreignMembership.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `an administrator lists members, and nobody else can`() {
        val c = Community()
        val (ana, person) = c.resident("Ana")

        val members = call(c.adminToken, HttpMethod.GET, "${c.base}/memberships", null, Array<MemberResponse>::class.java).body!!

        assertThat(members).hasSize(2)
        val linked = members.single { it.membershipId == ana.membershipId }
        assertThat(linked.personId).isEqualTo(person.id)
        assertThat(linked.email).endsWith("@example.com")
        assertThat(call(ana.token, HttpMethod.GET, "${c.base}/memberships", null, String::class.java).statusCode)
            .isEqualTo(HttpStatus.FORBIDDEN)
    }

    // Account

    @Test
    fun `an account holder changes their own name and language`() {
        val token = login("privacy-account-${sequence.incrementAndGet()}")

        val updated = call(token, HttpMethod.PUT, "/me", UpdateAccountRequest("Nombre Nuevo", "en"), AccountResponse::class.java)

        assertThat(updated.statusCode).isEqualTo(HttpStatus.OK)
        val read = call(token, HttpMethod.GET, "/me", null, AccountResponse::class.java).body!!
        assertThat(read.displayName).isEqualTo("Nombre Nuevo")
        assertThat(read.locale).isEqualTo("en")
    }

    @Test
    fun `a blank account name is rejected`() {
        val token = login("privacy-account-${sequence.incrementAndGet()}")

        val response = call(token, HttpMethod.PUT, "/me", UpdateAccountRequest(" ", "en"), String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
