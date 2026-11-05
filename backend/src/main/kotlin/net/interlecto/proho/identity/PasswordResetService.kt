package net.interlecto.proho.identity

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PasswordResetService(
    private val accounts: AccountRepository,
    private val resetTokens: PasswordResetTokenRepository,
    private val sessions: AccountSessionRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val passwordEncoder: PasswordEncoder,
    private val auditEventRecorder: AuditEventRecorder,
    private val authProperties: AuthProperties,
    private val notifier: PasswordResetNotifier,
) {
    private val eligibleStatuses =
        setOf(AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE, AccountStatus.LOCKED)

    /**
     * Hands the raw token to the notifier and returns it, or returns null when
     * the email doesn't match an eligible account. Never exposed over HTTP. The caller must respond the same
     * way (202, no body) either way, so this isn't an enumeration oracle.
     */
    @Transactional
    fun request(email: String): String? {
        val account = accounts.findByEmail(email) ?: return null
        if (account.status !in eligibleStatuses) return null

        val now = Instant.now()
        val rawToken = SecureToken.generate()
        val token =
            PasswordResetToken(
                id = Uuid7.generate(),
                accountId = account.id,
                tokenHash = SecureToken.hash(rawToken),
                createdAt = now,
                expiresAt = now.plus(authProperties.passwordResetTokenTtl),
                usedAt = null,
            )
        jdbcAggregateOperations.insert(token)

        auditEventRecorder.record(
            entityType = "account",
            entityId = account.id,
            action = "password_reset_requested",
            actorAccountId = account.id,
        )

        notifier.passwordResetRequested(account.id, rawToken)

        return rawToken
    }

    /** Returns false for a missing, used, or expired token — one outcome for all three. */
    @Transactional
    fun confirm(
        rawToken: String,
        newPassword: String,
    ): Boolean {
        val token = resetTokens.findByTokenHash(SecureToken.hash(rawToken)) ?: return false
        if (token.usedAt != null || token.expiresAt.isBefore(Instant.now())) return false
        val account = accounts.findById(token.accountId).orElse(null) ?: return false

        jdbcAggregateOperations.update(account.copy(passwordHash = passwordEncoder.encode(newPassword)!!))
        jdbcAggregateOperations.update(token.copy(usedAt = Instant.now()))

        sessions.findByAccountIdAndRevokedAtIsNull(account.id).forEach { session ->
            jdbcAggregateOperations.update(session.copy(revokedAt = Instant.now()))
        }

        auditEventRecorder.record(
            entityType = "account",
            entityId = account.id,
            action = "password_reset_confirmed",
            actorAccountId = account.id,
        )

        return true
    }
}
