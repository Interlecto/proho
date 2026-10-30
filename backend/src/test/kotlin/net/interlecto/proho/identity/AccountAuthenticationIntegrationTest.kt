package net.interlecto.proho.identity

import net.interlecto.proho.EmbeddedPostgres
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AccountAuthenticationIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    private fun registerAccount(email: String): AccountResponse {
        val response =
            rest.postForEntity(
                "/accounts",
                RegisterAccountRequest(email, "correct-horse-battery", "Test User"),
                AccountResponse::class.java,
            )
        return response.body!!
    }

    private fun authorized(token: String) = HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(token) })

    @Test
    fun `logs in with correct credentials and returns a bearer token`() {
        registerAccount("login-ok@example.com")

        val response =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("login-ok@example.com", "correct-horse-battery"),
                LoginResponse::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.token).isNotBlank()
        assertThat(response.body?.account?.email).isEqualTo("login-ok@example.com")
    }

    @Test
    fun `rejects a wrong password`() {
        registerAccount("login-wrong-pw@example.com")

        val response =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("login-wrong-pw@example.com", "not-the-password"),
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).contains("invalid_credentials")
    }

    @Test
    fun `rejects an unknown email with the same error as a wrong password`() {
        val response =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("no-such-account@example.com", "whatever-password"),
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).contains("invalid_credentials")
    }

    @Test
    fun `returns the authenticated account from GET me`() {
        registerAccount("me-ok@example.com")
        val login =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("me-ok@example.com", "correct-horse-battery"),
                LoginResponse::class.java,
            )

        val response =
            rest.exchange(
                "/me",
                HttpMethod.GET,
                authorized(login.body!!.token),
                AccountResponse::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.email).isEqualTo("me-ok@example.com")
    }

    @Test
    fun `rejects GET me with no authorization header`() {
        val response = rest.getForEntity("/me", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).contains("unauthenticated")
    }

    @Test
    fun `rejects GET me with a bogus token`() {
        val response =
            rest.exchange("/me", HttpMethod.GET, authorized("not-a-real-token"), String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `rejects GET me with an expired session token`() {
        val account = registerAccount("me-expired@example.com")
        val rawToken = SecureToken.generate()
        val now = Instant.now()
        jdbcAggregateOperations.insert(
            AccountSession(
                id = Uuid7.generate(),
                accountId = account.id,
                tokenHash = SecureToken.hash(rawToken),
                createdAt = now.minusSeconds(3600),
                expiresAt = now.minusSeconds(1),
                revokedAt = null,
            ),
        )

        val response =
            rest.exchange("/me", HttpMethod.GET, authorized(rawToken), String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `logout revokes the session immediately`() {
        registerAccount("logout-ok@example.com")
        val login =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("logout-ok@example.com", "correct-horse-battery"),
                LoginResponse::class.java,
            )
        val token = login.body!!.token

        val logoutResponse =
            rest.exchange("/auth/logout", HttpMethod.POST, authorized(token), Void::class.java)
        assertThat(logoutResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val meResponse = rest.exchange("/me", HttpMethod.GET, authorized(token), String::class.java)
        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
