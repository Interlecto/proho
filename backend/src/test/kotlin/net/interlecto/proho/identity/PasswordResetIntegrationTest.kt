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
class PasswordResetIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var passwordResetService: PasswordResetService

    @Autowired
    lateinit var resetTokens: PasswordResetTokenRepository

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
    fun `requesting a reset for a registered email is accepted and creates a token`() {
        val account = registerAccount("reset-ok@example.com")

        val response =
            rest.postForEntity(
                "/auth/password-reset/request",
                RequestPasswordResetRequest("reset-ok@example.com"),
                Void::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        assertThat(resetTokens.findAll().any { it.accountId == account.id }).isTrue()
    }

    @Test
    fun `requesting a reset for an unknown email returns the same status`() {
        val response =
            rest.postForEntity(
                "/auth/password-reset/request",
                RequestPasswordResetRequest("no-such-account@example.com"),
                Void::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
    }

    @Test
    fun `confirming with a valid token changes the password`() {
        registerAccount("reset-confirm@example.com")
        val rawToken = passwordResetService.request("reset-confirm@example.com")!!

        val confirmResponse =
            rest.postForEntity(
                "/auth/password-reset/confirm",
                ConfirmPasswordResetRequest(rawToken, "new-correct-horse-battery"),
                Void::class.java,
            )
        assertThat(confirmResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val loginWithNewPassword =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("reset-confirm@example.com", "new-correct-horse-battery"),
                LoginResponse::class.java,
            )
        assertThat(loginWithNewPassword.statusCode).isEqualTo(HttpStatus.OK)

        val loginWithOldPassword =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("reset-confirm@example.com", "correct-horse-battery"),
                String::class.java,
            )
        assertThat(loginWithOldPassword.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `rejects reusing an already-confirmed token`() {
        registerAccount("reset-reuse@example.com")
        val rawToken = passwordResetService.request("reset-reuse@example.com")!!
        passwordResetService.confirm(rawToken, "new-correct-horse-battery")

        val response =
            rest.postForEntity(
                "/auth/password-reset/confirm",
                ConfirmPasswordResetRequest(rawToken, "another-new-password"),
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).contains("invalid_or_expired_token")
    }

    @Test
    fun `rejects an expired token`() {
        val account = registerAccount("reset-expired@example.com")
        val rawToken = SecureToken.generate()
        val now = Instant.now()
        jdbcAggregateOperations.insert(
            PasswordResetToken(
                id = Uuid7.generate(),
                accountId = account.id,
                tokenHash = SecureToken.hash(rawToken),
                createdAt = now.minusSeconds(7200),
                expiresAt = now.minusSeconds(1),
                usedAt = null,
            ),
        )

        val response =
            rest.postForEntity(
                "/auth/password-reset/confirm",
                ConfirmPasswordResetRequest(rawToken, "another-new-password"),
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `confirming a reset revokes existing sessions`() {
        registerAccount("reset-revokes@example.com")
        val login =
            rest.postForEntity(
                "/auth/login",
                LoginRequest("reset-revokes@example.com", "correct-horse-battery"),
                LoginResponse::class.java,
            )
        val sessionToken = login.body!!.token

        val rawResetToken = passwordResetService.request("reset-revokes@example.com")!!
        passwordResetService.confirm(rawResetToken, "new-correct-horse-battery")

        val meResponse =
            rest.exchange("/me", HttpMethod.GET, authorized(sessionToken), String::class.java)
        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
