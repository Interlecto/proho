package net.interlecto.proho.audit

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.communities.AssignRoleRequest
import net.interlecto.proho.communities.AuditEventResponse
import net.interlecto.proho.communities.CommunityResponse
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.communities.CreateInvitationRequest
import net.interlecto.proho.communities.InvitationResponse
import net.interlecto.proho.communities.InvitationTokenRequest
import net.interlecto.proho.communities.MemberResponse
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.properties.CreateBuildingRequest
import net.interlecto.proho.shared.REQUEST_ID_HEADER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuditLogIntegrationTest {
    companion object {
        private val sequence = AtomicInteger()

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var recorder: AuditEventRecorder

    private fun <T : Any> call(
        token: String,
        method: HttpMethod,
        path: String,
        body: Any?,
        type: Class<T>,
        headers: HttpHeaders = HttpHeaders(),
    ): ResponseEntity<T> = rest.exchange(path, method, HttpEntity(body, headers.apply { setBearerAuth(token) }), type)

    private fun login(email: String): String {
        rest.postForEntity("/accounts", RegisterAccountRequest(email, "correct-horse-battery", "Nombre $email"), String::class.java)
        return rest.postForEntity("/auth/login", LoginRequest(email, "correct-horse-battery"), LoginResponse::class.java).body!!.token
    }

    private inner class Fixture {
        val tag = "audit-${sequence.incrementAndGet()}"
        val adminEmail = "$tag@example.com"
        val admin = login(adminEmail)
        val id: UUID =
            call(admin, HttpMethod.POST, "/communities", CreateCommunityRequest(tag, "Los Robles", kind = "ph_residential"), CommunityResponse::class.java)
                .body!!.id
        val base = "/communities/$id"
        private val members = AtomicInteger()

        fun building(name: String = "Torre") =
            call(admin, HttpMethod.POST, "$base/buildings", CreateBuildingRequest(name), String::class.java)

        fun events(
            token: String = admin,
            query: String = "",
        ) = call(token, HttpMethod.GET, "$base/audit-events$query", null, Array<AuditEventResponse>::class.java).body!!.toList()

        fun member(role: String? = null): String {
            val email = "$tag-m${members.incrementAndGet()}@example.com"
            val token = login(email)
            val invitation = call(admin, HttpMethod.POST, "$base/invitations", CreateInvitationRequest(email), InvitationResponse::class.java).body!!
            call(token, HttpMethod.POST, "/invitations/accept", InvitationTokenRequest(invitation.token!!), String::class.java)
            if (role != null) {
                val id =
                    call(admin, HttpMethod.GET, "$base/memberships", null, Array<MemberResponse>::class.java).body!!
                        .single { it.email == email }.membershipId
                call(admin, HttpMethod.POST, "$base/memberships/$id/roles", AssignRoleRequest(role), String::class.java)
            }
            return token
        }
    }

    // Request ids

    @Test
    fun `every response carries a request id, and the events it wrote share it`() {
        val f = Fixture()

        val response = f.building()

        val requestId = response.headers.getFirst(REQUEST_ID_HEADER)
        assertThat(requestId).isNotBlank()
        assertThat(f.events().first().requestId).isEqualTo(requestId)
        assertThat(f.events().filter { it.action == "founded" || (it.entityType == "community" && it.action == "created") }.map { it.requestId }.distinct())
            .hasSize(1)
    }

    @Test
    fun `each request gets its own id`() {
        val f = Fixture()

        val first = f.building().headers.getFirst(REQUEST_ID_HEADER)
        val second = f.building().headers.getFirst(REQUEST_ID_HEADER)

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `a client can't choose the request id`() {
        val f = Fixture()
        val forged = HttpHeaders().apply { set(REQUEST_ID_HEADER, "forged-id") }

        val response = call(f.admin, HttpMethod.POST, "${f.base}/buildings", CreateBuildingRequest("Torre"), String::class.java, forged)

        assertThat(response.headers.getFirst(REQUEST_ID_HEADER)).doesNotContain("forged")
        assertThat(f.events().first().requestId).doesNotContain("forged")
    }

    @Test
    fun `an event recorded outside a request has no request id`() {
        val event = recorder.record(entityType = "account", entityId = UUID.randomUUID(), action = "test")

        assertThat(event.requestId).isNull()
    }

    @Test
    fun `failed requests also carry an id`() {
        val f = Fixture()

        val response = rest.getForEntity("${f.base}/units", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.headers.getFirst(REQUEST_ID_HEADER)).isNotBlank()
    }

    // Reading

    @Test
    fun `an administrator reads the log newest first, with who did it`() {
        val f = Fixture()
        f.building("Primera")
        f.building("Segunda")

        val events = f.events()

        assertThat(events.first().summary).isEqualTo("Segunda")
        assertThat(events[1].summary).isEqualTo("Primera")
        assertThat(events.first().entityType).isEqualTo("building")
        assertThat(events.first().action).isEqualTo("created")
        assertThat(events.first().actorName).isEqualTo("Nombre ${f.adminEmail}")
    }

    @Test
    fun `an auditor can read it, and no other role can`() {
        val f = Fixture()
        f.building()

        assertThat(f.events(f.member("auditor"))).isNotEmpty()

        listOf("treasurer", "accountant", "council_member", "moderator", "staff", null).forEach {
            val response = call(f.member(it), HttpMethod.GET, "${f.base}/audit-events", null, String::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body).contains("role_not_held")
        }
    }

    @Test
    fun `another community can't read it, and platform events stay out of it`() {
        val f = Fixture()
        val other = Fixture()
        f.building("Secreta")

        val response = call(other.admin, HttpMethod.GET, "${f.base}/audit-events", null, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        assertThat(other.events().map { it.summary }).doesNotContain("Secreta")
        assertThat(f.events().map { it.action }).doesNotContain("registered", "logged_in", "login_succeeded")
        assertThat(f.events().map { it.entityType }).doesNotContain("account")
    }

    @Test
    fun `pages continue without gaps or repeats`() {
        val f = Fixture()
        repeat(5) { f.building("Torre $it") }

        val all = f.events()
        val first = f.events(query = "?limit=3")
        val second = f.events(query = "?limit=3&before=${first.last().id}")

        assertThat(first).hasSize(3)
        assertThat((first + second).map { it.id }).isEqualTo(all.map { it.id }.take(first.size + second.size))
        assertThat((first + second).map { it.id }).doesNotHaveDuplicates()
    }

    @Test
    fun `the page size has limits`() {
        val f = Fixture()
        repeat(2) { f.building() }

        assertThat(f.events(query = "?limit=0")).hasSize(1)
        assertThat(f.events(query = "?limit=100000")).hasSizeGreaterThan(2)
    }

    @Test
    fun `the application still can't change the log`() {
        val f = Fixture()
        f.building()
        val app = net.interlecto.proho.tenancy.TenantJdbc

        assertThat(runCatching { app.update("update audit_event set summary = 'x'") }.isFailure).isTrue()
        assertThat(runCatching { app.update("delete from audit_event") }.isFailure).isTrue()
    }
}
