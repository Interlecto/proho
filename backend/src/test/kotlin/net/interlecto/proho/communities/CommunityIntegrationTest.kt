package net.interlecto.proho.communities

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.tenancy.asCommunity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CommunityIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var memberships: MembershipRepository

    @Autowired
    lateinit var roleAssignments: RoleAssignmentRepository

    private fun registerAndLogin(email: String): String {
        rest.postForEntity(
            "/accounts",
            RegisterAccountRequest(email, "correct-horse-battery", "Test User"),
            String::class.java,
        )
        val login =
            rest.postForEntity("/auth/login", LoginRequest(email, "correct-horse-battery"), LoginResponse::class.java)
        return login.body!!.token
    }

    private fun authorized(token: String) = HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(token) })

    private fun createCommunity(
        token: String,
        key: String,
    ): CommunityResponse {
        val headers = HttpHeaders().apply { setBearerAuth(token) }
        val request = HttpEntity(CreateCommunityRequest(key = key, name = "Los Robles", kind = "ph_residential"), headers)
        return rest.postForEntity("/communities", request, CommunityResponse::class.java).body!!
    }

    @Test
    fun `creating a community makes the creator its administrator`() {
        val token = registerAndLogin("found-ok@example.com")

        val community = createCommunity(token, "los-robles-1")

        assertThat(community.key).isEqualTo("los-robles-1")

        asCommunity(community.id) {
            val membership = memberships.findAll().single()
            assertThat(membership.status).isEqualTo(MembershipStatus.ACTIVE)
            assertThat(roleAssignments.findByMembershipIdAndRoleAndValidToIsNull(membership.id, Role.ADMINISTRATOR))
                .isNotNull()
        }
    }

    @Test
    fun `lists only my communities`() {
        val tokenA = registerAndLogin("list-a@example.com")
        val tokenB = registerAndLogin("list-b@example.com")
        createCommunity(tokenA, "mine-a")
        createCommunity(tokenB, "mine-b")

        val response =
            rest.exchange("/communities", org.springframework.http.HttpMethod.GET, authorized(tokenA), Array<CommunityResponse>::class.java)

        val keys = response.body!!.map { it.key }
        assertThat(keys).contains("mine-a").doesNotContain("mine-b")
    }

    @Test
    fun `rejects a duplicate community key`() {
        val token = registerAndLogin("dup-key@example.com")
        createCommunity(token, "duplicate-key")

        val headers = HttpHeaders().apply { setBearerAuth(token) }
        val request =
            HttpEntity(CreateCommunityRequest(key = "duplicate-key", name = "Other", kind = "ph_residential"), headers)
        val response = rest.postForEntity("/communities", request, String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }
}
