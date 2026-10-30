package net.interlecto.proho.residents

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.MembershipStatus
import net.interlecto.proho.identity.AccountResponse
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.properties.CreateUnitRequest
import net.interlecto.proho.properties.UnitResponse
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ResidentIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    private fun authorizedHeaders(token: String) = HttpHeaders().apply { setBearerAuth(token) }

    private fun accountId(token: String): UUID {
        val response =
            rest.exchange("/me", HttpMethod.GET, HttpEntity<Void>(authorizedHeaders(token)), AccountResponse::class.java)
        return response.body!!.id
    }

    private fun addPlainActiveMembership(
        token: String,
        communityId: UUID,
    ) {
        jdbcAggregateOperations.insert(
            Membership(
                id = Uuid7.generate(),
                accountId = accountId(token),
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

    private fun createUnit(
        token: String,
        communityId: UUID,
        number: String,
    ): UUID {
        val response =
            rest.postForEntity(
                "/communities/$communityId/units",
                HttpEntity(CreateUnitRequest(number = number, kind = "apartment"), authorizedHeaders(token)),
                UnitResponse::class.java,
            )
        return response.body!!.id
    }

    private fun createPerson(
        token: String,
        communityId: UUID,
        givenName: String,
    ): UUID {
        val response =
            rest.postForEntity(
                "/communities/$communityId/people",
                HttpEntity(
                    CreatePersonRequest(kind = "natural", givenName = givenName, surname = "Test"),
                    authorizedHeaders(token),
                ),
                PersonResponse::class.java,
            )
        return response.body!!.id
    }

    @Test
    fun `administrator can create a person and a unit relationship`() {
        val token = registerAndLogin("res-admin@example.com")
        val communityId = createCommunity(token, "res-admin-community")
        val unitId = createUnit(token, communityId, "100")
        val personId = createPerson(token, communityId, "Ana")

        val response =
            rest.postForEntity(
                "/communities/$communityId/units/$unitId/relationships",
                HttpEntity(
                    CreateUnitRelationshipRequest(personId = personId, role = "resident"),
                    authorizedHeaders(token),
                ),
                UnitRelationshipResponse::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.role).isEqualTo("resident")
    }

    @Test
    fun `non-administrator member can read but not write people`() {
        val adminToken = registerAndLogin("res-member-admin@example.com")
        val communityId = createCommunity(adminToken, "res-member-community")
        val memberToken = registerAndLogin("res-plain-member@example.com")
        addPlainActiveMembership(memberToken, communityId)

        val listResponse =
            rest.exchange(
                "/communities/$communityId/people",
                HttpMethod.GET,
                HttpEntity<Void>(authorizedHeaders(memberToken)),
                String::class.java,
            )
        assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)

        val createResponse =
            rest.postForEntity(
                "/communities/$communityId/people",
                HttpEntity(CreatePersonRequest(kind = "natural", givenName = "X", surname = "Y"), authorizedHeaders(memberToken)),
                String::class.java,
            )
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `co-owner shares summing to 100 succeed but exceeding 100 is rejected`() {
        val token = registerAndLogin("res-owner-admin@example.com")
        val communityId = createCommunity(token, "res-owner-community")
        val unitId = createUnit(token, communityId, "200")
        val owner1 = createPerson(token, communityId, "Owner1")
        val owner2 = createPerson(token, communityId, "Owner2")
        val owner3 = createPerson(token, communityId, "Owner3")

        val first =
            rest.postForEntity(
                "/communities/$communityId/units/$unitId/relationships",
                HttpEntity(
                    CreateUnitRelationshipRequest(personId = owner1, role = "owner", share = BigDecimal("60")),
                    authorizedHeaders(token),
                ),
                String::class.java,
            )
        assertThat(first.statusCode).isEqualTo(HttpStatus.CREATED)

        val second =
            rest.postForEntity(
                "/communities/$communityId/units/$unitId/relationships",
                HttpEntity(
                    CreateUnitRelationshipRequest(personId = owner2, role = "owner", share = BigDecimal("40")),
                    authorizedHeaders(token),
                ),
                String::class.java,
            )
        assertThat(second.statusCode).isEqualTo(HttpStatus.CREATED)

        val third =
            rest.postForEntity(
                "/communities/$communityId/units/$unitId/relationships",
                HttpEntity(
                    CreateUnitRelationshipRequest(personId = owner3, role = "owner", share = BigDecimal("1")),
                    authorizedHeaders(token),
                ),
                String::class.java,
            )
        assertThat(third.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `rejects a duplicate identity document within the same community`() {
        val token = registerAndLogin("res-id-admin@example.com")
        val communityId = createCommunity(token, "res-id-community")

        val first =
            rest.postForEntity(
                "/communities/$communityId/people",
                HttpEntity(
                    CreatePersonRequest(kind = "natural", givenName = "A", surname = "B", idType = "CC", idNumber = "123"),
                    authorizedHeaders(token),
                ),
                String::class.java,
            )
        assertThat(first.statusCode).isEqualTo(HttpStatus.CREATED)

        val duplicate =
            rest.postForEntity(
                "/communities/$communityId/people",
                HttpEntity(
                    CreatePersonRequest(kind = "natural", givenName = "C", surname = "D", idType = "CC", idNumber = "123"),
                    authorizedHeaders(token),
                ),
                String::class.java,
            )
        assertThat(duplicate.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }
}
