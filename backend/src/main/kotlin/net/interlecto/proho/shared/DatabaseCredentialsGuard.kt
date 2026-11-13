package net.interlecto.proho.shared

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Refuses to start with database passwords that look like development ones:
 * missing, short, equal to the role name, a template placeholder, or shared
 * by the two roles. Runs while Flyway is being configured, so before the
 * first migration connects. Local development and tests opt out with
 * `proho.security.allow-development-credentials`. See
 * docs/security/secrets.md.
 */
@Component
class DatabaseCredentialsGuard(
    private val environment: Environment,
) : FlywayConfigurationCustomizer {
    override fun customize(configuration: FluentConfiguration) {
        if (environment.getProperty(ALLOW_DEVELOPMENT_CREDENTIALS, Boolean::class.java, false)) return

        val application = login("PROHO_DB_USER", "spring.datasource.username", "PROHO_DB_PASSWORD", "spring.datasource.password")
        val migration =
            login("PROHO_DB_MIGRATION_USER", "spring.flyway.user", "PROHO_DB_MIGRATION_PASSWORD", "spring.flyway.password")
        // A missing setting is reported by Spring itself, naming the variable.
        if (application == null || migration == null) return

        val found = problems(application, migration)
        check(found.isEmpty()) {
            "Refusing to start with development database credentials: " + found.joinToString("; ") +
                ". Use generated passwords (docs/security/secrets.md). Only local development and tests may set " +
                "$ALLOW_DEVELOPMENT_CREDENTIALS=true."
        }
    }

    private fun login(
        userVariable: String,
        userProperty: String,
        passwordVariable: String,
        passwordProperty: String,
    ): DatabaseLogin? =
        try {
            DatabaseLogin(userVariable, environment.getProperty(userProperty), passwordVariable, environment.getProperty(passwordProperty))
        } catch (_: IllegalArgumentException) {
            null
        }

    /** Never holds the password in its text: only variable names go into messages. */
    data class DatabaseLogin(
        val userVariable: String,
        val user: String?,
        val passwordVariable: String,
        val password: String?,
    )

    companion object {
        const val ALLOW_DEVELOPMENT_CREDENTIALS = "proho.security.allow-development-credentials"
        const val MINIMUM_LENGTH = 16

        private val placeholderWords = listOf("changeme", "change-me", "change_me", "password", "secret", "example")

        // A template's "<...>", like backend/README.md's '<secret>'. Not a lone
        // "<" or ">", which a generated password may contain.
        private val templateMarker = Regex("<[^>]*>")

        private fun looksLikePlaceholder(password: String) =
            placeholderWords.any { password.contains(it, ignoreCase = true) } || templateMarker.containsMatchIn(password)

        fun problems(
            application: DatabaseLogin,
            migration: DatabaseLogin,
        ): List<String> {
            val found = mutableListOf<String>()
            for (login in listOf(application, migration)) {
                val password = login.password
                val name = login.passwordVariable
                when {
                    password.isNullOrBlank() -> found += "$name is empty"
                    password.length < MINIMUM_LENGTH -> found += "$name is shorter than $MINIMUM_LENGTH characters"
                    login.user != null && password.equals(login.user, ignoreCase = true) ->
                        found += "$name is the role's name"
                    looksLikePlaceholder(password) ->
                        found += "$name looks like a placeholder"
                }
            }
            if (!application.password.isNullOrBlank() && application.password == migration.password) {
                found += "${application.passwordVariable} and ${migration.passwordVariable} are the same"
            }
            if (application.user != null && application.user.equals(migration.user, ignoreCase = true)) {
                found += "${application.userVariable} and ${migration.userVariable} are the same role"
            }
            return found
        }
    }
}
