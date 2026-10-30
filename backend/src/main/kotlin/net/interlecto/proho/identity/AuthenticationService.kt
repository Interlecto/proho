package net.interlecto.proho.identity

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class LoginResult(
    val token: String,
    val expiresAt: Instant,
    val account: Account,
)

class InvalidCredentialsException : RuntimeException("Invalid email or password")

@Service
class AuthenticationService(
    private val accounts: AccountRepository,
    private val sessions: AccountSessionRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val passwordEncoder: PasswordEncoder,
    private val auditEventRecorder: AuditEventRecorder,
    private val authProperties: AuthProperties,
) {
    private val rejectedStatuses = setOf(AccountStatus.LOCKED, AccountStatus.CLOSED)

    // A bcrypt hash of a password nobody will ever type, computed once.
    // Matching against it when the email is unknown keeps the response
    // time comparable to a real wrong-password attempt, so neither
    // outcome is distinguishable by timing.
    private val dummyHash = passwordEncoder.encode(Uuid7.generate().toString())!!

    @Transactional
    fun login(
        email: String,
        password: String,
    ): LoginResult {
        val account = accounts.findByEmail(email)
        val passwordMatches = passwordEncoder.matches(password, account?.passwordHash ?: dummyHash)
        if (account == null || !passwordMatches || account.status in rejectedStatuses) {
            throw InvalidCredentialsException()
        }

        val now = Instant.now()
        val rawToken = SecureToken.generate()
        val session =
            AccountSession(
                id = Uuid7.generate(),
                accountId = account.id,
                tokenHash = SecureToken.hash(rawToken),
                createdAt = now,
                expiresAt = now.plus(authProperties.sessionTtl),
                revokedAt = null,
            )
        val saved = jdbcAggregateOperations.insert(session)

        auditEventRecorder.record(
            entityType = "account_session",
            entityId = saved.id,
            action = "login_succeeded",
            actorAccountId = account.id,
        )

        return LoginResult(token = rawToken, expiresAt = saved.expiresAt, account = account)
    }

    @Transactional
    fun logout(sessionId: UUID) {
        val session = sessions.findById(sessionId).orElse(null) ?: return
        if (session.revokedAt != null) return
        val revoked = session.copy(revokedAt = Instant.now())
        jdbcAggregateOperations.update(revoked)
        auditEventRecorder.record(
            entityType = "account_session",
            entityId = session.id,
            action = "logout",
            actorAccountId = session.accountId,
        )
    }
}
