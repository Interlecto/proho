package net.interlecto.proho.identity

import net.interlecto.proho.EmbeddedPostgres
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AccountRegistrationIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var accounts: AccountRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `registers an account with a hashed password, pending verification`() {
        val response =
            rest.postForEntity(
                "/accounts",
                RegisterAccountRequest("ana@example.com", "correct-horse-battery", "Ana"),
                AccountResponse::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.status).isEqualTo("pending_verification")

        val saved = accounts.findByEmail("ana@example.com")!!
        assertThat(saved.passwordHash).isNotEqualTo("correct-horse-battery")
        assertThat(passwordEncoder.matches("correct-horse-battery", saved.passwordHash)).isTrue()
    }

    @Test
    fun `rejects a duplicate email`() {
        rest.postForEntity(
            "/accounts",
            RegisterAccountRequest("dup@example.com", "correct-horse-battery", "Dup"),
            AccountResponse::class.java,
        )

        val second =
            rest.postForEntity(
                "/accounts",
                RegisterAccountRequest("dup@example.com", "another-password", "Dup Again"),
                String::class.java,
            )

        assertThat(second.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `rejects an invalid email`() {
        val response =
            rest.postForEntity(
                "/accounts",
                RegisterAccountRequest("not-an-email", "correct-horse-battery", "X"),
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `rejects a password shorter than 8 characters`() {
        val response =
            rest.postForEntity(
                "/accounts",
                RegisterAccountRequest("short@example.com", "short", "Short"),
                String::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
