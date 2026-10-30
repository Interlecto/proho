package net.interlecto.proho.properties

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.MembershipStatus
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.shared.Uuid7
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class PropertyIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    private fun accountId(token: String): UUID {
        val response =
            rest.exchange(
                "/me",
                HttpMethod.GET,
                HttpEntity<Void>(authorizedHeaders(token)),
                net.interlecto.proho.identity.AccountResponse::class.java,
            )
        return response.body!!.id
    }

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

    private fun authorizedHeaders(token: String) = HttpHeaders().apply { setBearerAuth(token) }

    private fun createCommunity(
        token: String,
        key: String,
    ): UUID {
        val request =
            HttpEntity(
                CreateCommunityRequest(key = key, name = "Los Robles", kind = "ph_residential"),
                authorizedHeaders(token),
            )
        val response =
            rest.postForEntity("/communities", request, net.interlecto.proho.communities.CommunityResponse::class.java)
        return response.body!!.id
    }

    @Test
    fun `administrator can create a building and a unit`() {
        val token = registerAndLogin("prop-admin@example.com")
        val communityId = createCommunity(token, "prop-admin-community")

        val buildingResponse =
            rest.postForEntity(
                "/communities/$communityId/buildings",
                HttpEntity(CreateBuildingRequest("Torre 1"), authorizedHeaders(token)),
                BuildingResponse::class.java,
            )
        assertThat(buildingResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val unitResponse =
            rest.postForEntity(
                "/communities/$communityId/units",
                HttpEntity(
                    CreateUnitRequest(number = "101", kind = "apartment", buildingId = buildingResponse.body!!.id),
                    authorizedHeaders(token),
                ),
                UnitResponse::class.java,
            )
        assertThat(unitResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(unitResponse.body!!.number).isEqualTo("101")
    }

    @Test
    fun `an account with no membership is forbidden`() {
        val adminToken = registerAndLogin("prop-isolated-admin@example.com")
        val communityId = createCommunity(adminToken, "prop-isolated-community")
        val outsiderToken = registerAndLogin("prop-outsider@example.com")

        val createResponse =
            rest.postForEntity(
                "/communities/$communityId/units",
                HttpEntity(CreateUnitRequest(number = "1", kind = "apartment"), authorizedHeaders(outsiderToken)),
                String::class.java,
            )
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val listResponse =
            rest.exchange(
                "/communities/$communityId/units",
                HttpMethod.GET,
                HttpEntity<Void>(authorizedHeaders(outsiderToken)),
                String::class.java,
            )
        assertThat(listResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `a non-administrator active member can read but not write`() {
        val adminToken = registerAndLogin("prop-member-admin@example.com")
        val communityId = createCommunity(adminToken, "prop-member-community")
        val memberToken = registerAndLogin("prop-plain-member@example.com")
        jdbcAggregateOperations.insert(
            Membership(
                id = Uuid7.generate(),
                accountId = accountId(memberToken),
                communityId = communityId,
                personId = null,
                status = MembershipStatus.ACTIVE,
                invitedBy = null,
                invitedAt = null,
                joinedAt = Instant.now(),
                endedAt = null,
                createdAt = Instant.now(),
            ),
        )

        val listResponse =
            rest.exchange(
                "/communities/$communityId/units",
                HttpMethod.GET,
                HttpEntity<Void>(authorizedHeaders(memberToken)),
                String::class.java,
            )
        assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)

        val createResponse =
            rest.postForEntity(
                "/communities/$communityId/units",
                HttpEntity(CreateUnitRequest(number = "1", kind = "apartment"), authorizedHeaders(memberToken)),
                String::class.java,
            )
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `rejects a duplicate unit number within the same community but allows it in another`() {
        val token = registerAndLogin("prop-dup-admin@example.com")
        val communityId1 = createCommunity(token, "prop-dup-community-1")
        val communityId2 = createCommunity(token, "prop-dup-community-2")

        rest.postForEntity(
            "/communities/$communityId1/units",
            HttpEntity(CreateUnitRequest(number = "500", kind = "apartment"), authorizedHeaders(token)),
            UnitResponse::class.java,
        )

        val duplicate =
            rest.postForEntity(
                "/communities/$communityId1/units",
                HttpEntity(CreateUnitRequest(number = "500", kind = "apartment"), authorizedHeaders(token)),
                String::class.java,
            )
        assertThat(duplicate.statusCode).isEqualTo(HttpStatus.CONFLICT)

        val otherCommunity =
            rest.postForEntity(
                "/communities/$communityId2/units",
                HttpEntity(CreateUnitRequest(number = "500", kind = "apartment"), authorizedHeaders(token)),
                UnitResponse::class.java,
            )
        assertThat(otherCommunity.statusCode).isEqualTo(HttpStatus.CREATED)
    }
}
