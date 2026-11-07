package net.interlecto.proho.tenancy

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.communities.CommunityResponse
import net.interlecto.proho.communities.CreateCommunityRequest
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.properties.BuildingResponse
import net.interlecto.proho.properties.CreateBuildingRequest
import net.interlecto.proho.properties.CreateUnitRequest
import net.interlecto.proho.properties.UnitResponse
import net.interlecto.proho.residents.CreatePersonRequest
import net.interlecto.proho.residents.CreateUnitRelationshipRequest
import net.interlecto.proho.residents.PersonResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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

/**
 * Two communities, each with its own administrator, and every endpoint that
 * takes a community or row id tried across them. Administrator of A is the
 * interesting attacker: a real, privileged, authenticated user who only needs
 * to pass community A's checks and an id from community B.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CrossCommunityIsolationIntegrationTest {
    companion object {
        private val sequence = AtomicInteger()

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    private data class Community(
        val adminToken: String,
        val id: UUID,
        val buildingId: UUID,
        val unitId: UUID,
        val personId: UUID,
    )

    private lateinit var a: Community
    private lateinit var b: Community

    private fun headers(token: String) = HttpHeaders().apply { setBearerAuth(token) }

    private fun <T : Any> post(
        path: String,
        body: Any,
        token: String,
        type: Class<T>,
    ): ResponseEntity<T> = rest.postForEntity(path, HttpEntity(body, headers(token)), type)

    private fun get(
        path: String,
        token: String,
    ): ResponseEntity<String> = rest.exchange(path, HttpMethod.GET, HttpEntity<Void>(headers(token)), String::class.java)

    private fun community(): Community {
        val n = sequence.incrementAndGet()
        val email = "isolation-$n@example.com"
        rest.postForEntity("/accounts", RegisterAccountRequest(email, "correct-horse-battery", "Admin $n"), String::class.java)
        val token =
            rest.postForEntity("/auth/login", LoginRequest(email, "correct-horse-battery"), LoginResponse::class.java).body!!.token
        val id =
            post(
                "/communities",
                CreateCommunityRequest(key = "isolation-$n", name = "Community $n", kind = "ph_residential"),
                token,
                CommunityResponse::class.java,
            ).body!!.id
        val buildingId =
            post("/communities/$id/buildings", CreateBuildingRequest("Torre $n"), token, BuildingResponse::class.java).body!!.id
        val unitId =
            post(
                "/communities/$id/units",
                CreateUnitRequest(number = "u-$n", kind = "apartment", buildingId = buildingId),
                token,
                UnitResponse::class.java,
            ).body!!.id
        val personId =
            post(
                "/communities/$id/people",
                CreatePersonRequest(kind = "natural", givenName = "Person$n", surname = "Secret$n"),
                token,
                PersonResponse::class.java,
            ).body!!.id
        return Community(token, id, buildingId, unitId, personId)
    }

    @BeforeEach
    fun twoCommunities() {
        a = community()
        b = community()
    }

    @Test
    fun `a unit can't be created in another community's building`() {
        val response =
            post(
                "/communities/${a.id}/units",
                CreateUnitRequest(number = "stolen", kind = "apartment", buildingId = b.buildingId),
                a.adminToken,
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).contains("not_found_in_community")
        assertThat(get("/communities/${a.id}/units", a.adminToken).body).doesNotContain("stolen")
    }

    @Test
    fun `a unit can't be created in a building that doesn't exist`() {
        val response =
            post(
                "/communities/${a.id}/units",
                CreateUnitRequest(number = "nowhere", kind = "apartment", buildingId = UUID.randomUUID()),
                a.adminToken,
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).contains("not_found_in_community")
    }

    @Test
    fun `a relationship can't use another community's unit or person`() {
        val otherUnit =
            post(
                "/communities/${a.id}/units/${b.unitId}/relationships",
                CreateUnitRelationshipRequest(personId = a.personId, role = "resident"),
                a.adminToken,
                String::class.java,
            )
        assertThat(otherUnit.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(otherUnit.body).contains("not_found_in_community")

        val otherPerson =
            post(
                "/communities/${a.id}/units/${a.unitId}/relationships",
                CreateUnitRelationshipRequest(personId = b.personId, role = "resident"),
                a.adminToken,
                String::class.java,
            )
        assertThat(otherPerson.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(otherPerson.body).contains("not_found_in_community")
    }

    @Test
    fun `another community's relationships can't be read through your own`() {
        post(
            "/communities/${b.id}/units/${b.unitId}/relationships",
            CreateUnitRelationshipRequest(personId = b.personId, role = "resident"),
            b.adminToken,
            String::class.java,
        )

        val response = get("/communities/${a.id}/units/${b.unitId}/relationships", a.adminToken)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).doesNotContain(b.personId.toString())
    }

    @Test
    fun `another community's reads are forbidden, even to an administrator elsewhere`() {
        val paths =
            listOf(
                "/communities/${b.id}/buildings",
                "/communities/${b.id}/units",
                "/communities/${b.id}/people",
                "/communities/${b.id}/units/${b.unitId}/relationships",
            )

        paths.forEach { path ->
            val response = get(path, a.adminToken)
            assertThat(response.statusCode).describedAs(path).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body).describedAs(path).contains("membership_not_active")
            assertThat(response.body).describedAs(path).doesNotContain("Secret")
        }
    }

    @Test
    fun `another community's writes are forbidden, even to an administrator elsewhere`() {
        val writes: List<Pair<String, Any>> =
            listOf(
                "/communities/${b.id}/buildings" to CreateBuildingRequest("Intruder"),
                "/communities/${b.id}/units" to CreateUnitRequest(number = "intruder", kind = "apartment"),
                "/communities/${b.id}/people" to CreatePersonRequest(kind = "natural", givenName = "In", surname = "Truder"),
                "/communities/${b.id}/units/${b.unitId}/relationships" to
                    CreateUnitRelationshipRequest(personId = b.personId, role = "resident"),
            )

        writes.forEach { (path, body) ->
            val response = post(path, body, a.adminToken, String::class.java)
            assertThat(response.statusCode).describedAs(path).isEqualTo(HttpStatus.FORBIDDEN)
        }
        assertThat(get("/communities/${b.id}/units", b.adminToken).body).doesNotContain("intruder")
        assertThat(get("/communities/${b.id}/people", b.adminToken).body).doesNotContain("Truder")
    }

    @Test
    fun `a community key taken by someone else is still rejected`() {
        val keyOfB = "shared-key-${sequence.incrementAndGet()}"
        post(
            "/communities",
            CreateCommunityRequest(key = keyOfB, name = "First", kind = "ph_residential"),
            b.adminToken,
            CommunityResponse::class.java,
        )

        // Row-level security hides B's community from A, so this only works
        // because the unique index sees through it.
        val response =
            post(
                "/communities",
                CreateCommunityRequest(key = keyOfB, name = "Second", kind = "ph_residential"),
                a.adminToken,
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).contains("community_key_already_registered")
    }
}
