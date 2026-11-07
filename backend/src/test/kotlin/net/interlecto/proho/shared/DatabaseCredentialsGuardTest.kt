package net.interlecto.proho.shared

import net.interlecto.proho.ProHoApplication
import net.interlecto.proho.shared.DatabaseCredentialsGuard.Companion.problems
import net.interlecto.proho.shared.DatabaseCredentialsGuard.DatabaseLogin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assumptions.assumeThat
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

class DatabaseCredentialsGuardTest {
    private val strongApp = "kV9u2#pQx7LmW4zT"
    private val strongOwner = "Hb3!rN8sYc6eJq1D"

    private fun app(
        password: String?,
        user: String = "proho_app",
    ) = DatabaseLogin("PROHO_DB_USER", user, "PROHO_DB_PASSWORD", password)

    private fun owner(
        password: String?,
        user: String = "proho",
    ) = DatabaseLogin("PROHO_DB_MIGRATION_USER", user, "PROHO_DB_MIGRATION_PASSWORD", password)

    @Test
    fun `generated passwords for two different roles pass`() {
        assertThat(problems(app(strongApp), owner(strongOwner))).isEmpty()
    }

    @Test
    fun `the development defaults fail`() {
        val found = problems(app("proho_app"), owner("proho"))

        assertThat(found).contains(
            "PROHO_DB_PASSWORD is shorter than 16 characters",
            "PROHO_DB_MIGRATION_PASSWORD is shorter than 16 characters",
        )
    }

    @Test
    fun `empty, placeholder and role-name passwords fail`() {
        assertThat(problems(app(""), owner(strongOwner))).containsExactly("PROHO_DB_PASSWORD is empty")
        assertThat(problems(app(null), owner(strongOwner))).containsExactly("PROHO_DB_PASSWORD is empty")
        assertThat(problems(app("<generate-a-password-here>"), owner(strongOwner)))
            .containsExactly("PROHO_DB_PASSWORD looks like a placeholder")
        assertThat(problems(app("changeme-changeme-changeme"), owner(strongOwner)))
            .containsExactly("PROHO_DB_PASSWORD looks like a placeholder")
        assertThat(problems(app("a-very-long-role-name", user = "a-very-long-role-name"), owner(strongOwner)))
            .containsExactly("PROHO_DB_PASSWORD is the role's name")
    }

    @Test
    fun `the two roles can't share a password or be the same role`() {
        assertThat(problems(app(strongApp), owner(strongApp)))
            .containsExactly("PROHO_DB_PASSWORD and PROHO_DB_MIGRATION_PASSWORD are the same")
        assertThat(problems(app(strongApp, user = "proho"), owner(strongOwner)))
            .containsExactly("PROHO_DB_USER and PROHO_DB_MIGRATION_USER are the same role")
    }

    @Test
    fun `messages never contain the password`() {
        val found = problems(app("short-secret-x"), owner("short-secret-x"))

        assertThat(found.joinToString()).doesNotContain("short-secret-x")
    }

    @Test
    fun `the application refuses to start with weak passwords, before connecting`() {
        assumeThat(System.getenv("PROHO_DB_URL")).isNull()
        // Nothing listens on port 1: getting this far without a connection
        // error shows the check runs before Flyway connects.
        val url = "jdbc:postgresql://localhost:1/proho"

        assertThatThrownBy {
            SpringApplicationBuilder(ProHoApplication::class.java)
                .web(WebApplicationType.NONE)
                // Arguments, not .properties(): those are defaults, which
                // application.yml's ${PROHO_DB_*} placeholders override.
                .run(
                    "--spring.datasource.url=$url",
                    "--spring.datasource.username=proho_app",
                    "--spring.datasource.password=proho_app",
                    "--spring.flyway.url=$url",
                    "--spring.flyway.user=proho",
                    "--spring.flyway.password=proho",
                ).close()
        }.hasStackTraceContaining("Refusing to start with development database credentials")
            .hasStackTraceContaining("PROHO_DB_PASSWORD is shorter than 16 characters")
            .hasStackTraceContaining("docs/security/secrets.md")
    }
}
