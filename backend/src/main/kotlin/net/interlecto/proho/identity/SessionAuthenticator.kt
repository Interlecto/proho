package net.interlecto.proho.identity

import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Resolves a raw bearer token to a principal. Returns null on any failure
 * (missing, revoked, expired, or an ineligible account status) rather than
 * throwing, so BearerTokenAuthenticationFilter just leaves the request
 * unauthenticated and authorizeHttpRequests produces the 401.
 */
@Service
class SessionAuthenticator(
    private val sessions: AccountSessionRepository,
    private val accounts: AccountRepository,
) {
    private val eligibleStatuses = setOf(AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE)

    fun authenticate(rawToken: String): AuthenticatedAccount? {
        val session = sessions.findByTokenHash(SecureToken.hash(rawToken)) ?: return null
        if (session.revokedAt != null || session.expiresAt.isBefore(Instant.now())) return null
        val account = accounts.findById(session.accountId).orElse(null) ?: return null
        if (account.status !in eligibleStatuses) return null
        return AuthenticatedAccount(
            accountId = account.id,
            sessionId = session.id,
            email = account.email,
            displayName = account.displayName,
            locale = account.locale,
            status = account.status,
        )
    }
}
